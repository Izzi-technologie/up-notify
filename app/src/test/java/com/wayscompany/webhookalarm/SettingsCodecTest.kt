package com.wayscompany.webhookalarm

import com.wayscompany.webhookalarm.settings.AlertPolicies
import com.wayscompany.webhookalarm.settings.AppSettings
import com.wayscompany.webhookalarm.settings.SettingsCodec
import com.wayscompany.webhookalarm.utils.deriveWebSocketUrl
import com.wayscompany.webhookalarm.utils.deriveWebhookUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsCodecTest {
    @Test
    fun roundTripKeepsPolicies() {
        val settings = AppSettings(
            serverUrl = "https://alarm.example.com",
            deviceId = "K7M2P",
            webSocketUrl = "wss://alarm.example.com/ws",
            authToken = "later",
            setupCompleted = true,
            policies = AlertPolicies.Default.copy(
                info = AlertPolicies.Default.info.copy(volumePercent = 35, repeatCount = 2),
                critical = AlertPolicies.Default.critical.copy(persistent = true, requireAck = true),
            ),
        )
        assertEquals(settings, SettingsCodec.decode(SettingsCodec.encode(settings)))
    }

    @Test
    fun missingKeysUseDefaults() {
        val decoded = SettingsCodec.decode(emptyMap())
        assertEquals(AppSettings(), decoded)
        assertEquals(40, decoded.policies.info.volumePercent)
        assertEquals(75, decoded.policies.warning.volumePercent)
        assertEquals(100, decoded.policies.critical.volumePercent)
        assertEquals(false, decoded.setupCompleted)
        assertEquals("", decoded.deviceId)
    }

    @Test
    fun storedDeviceIdIsKeptUntilTheRuntimeReplacesIt() {
        val decoded = SettingsCodec.decode(mapOf("device_id" to "device-001"))
        assertEquals("device-001", decoded.deviceId)
    }

    @Test
    fun invalidNumbersFallBack() {
        val decoded = SettingsCodec.decode(
            mapOf(
                "info_volume" to "loud",
                "warning_repeat" to "0",
                "critical_persistent" to "true",
            ),
        )
        assertEquals(40, decoded.policies.info.volumePercent)
        assertEquals(1, decoded.policies.warning.repeatCount)
        assertEquals(true, decoded.policies.critical.persistent)
    }

    @Test
    fun derivesWebhookUrl() {
        assertEquals(
            "https://alarm.example.com/webhook/K7M2P",
            deriveWebhookUrl("https://alarm.example.com", "K7M2P"),
        )
        assertEquals(
            "https://alarm.example.com/webhook/K7M2P",
            deriveWebhookUrl("wss://alarm.example.com/ws", "K7M2P"),
        )
        assertEquals(
            "http://10.0.0.8:8080/webhook/B4NQ8",
            deriveWebhookUrl("http://10.0.0.8:8080", "B4NQ8"),
        )
    }

    @Test
    fun derivesWebSocketUrl() {
        assertEquals("wss://alarm.example.com/ws", deriveWebSocketUrl("https://alarm.example.com"))
        assertEquals("wss://alarm.example.com/ws", deriveWebSocketUrl("https://alarm.example.com/"))
        assertEquals("ws://10.0.0.8:8080/ws", deriveWebSocketUrl("http://10.0.0.8:8080"))
        assertEquals("wss://alarm.example.com/ws", deriveWebSocketUrl("wss://alarm.example.com/ws"))
        assertEquals("wss://alarm.example.com/ws", deriveWebSocketUrl("alarm.example.com"))
    }
}
