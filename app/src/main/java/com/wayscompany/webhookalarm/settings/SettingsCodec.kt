package com.wayscompany.webhookalarm.settings

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wayscompany.webhookalarm.utils.deriveWebSocketUrl

object SettingsCodec {
    private const val SERVER_URL = "server_url"
    private const val DEVICE_ID = "device_id"
    private const val WEBSOCKET_URL = "websocket_url"
    private const val AUTH_TOKEN = "auth_token"
    private const val SETUP_COMPLETED = "setup_completed"

    fun encode(settings: AppSettings): Map<String, String> = buildMap {
        put(SERVER_URL, settings.serverUrl)
        put(DEVICE_ID, settings.deviceId)
        put(WEBSOCKET_URL, settings.webSocketUrl)
        put(AUTH_TOKEN, settings.authToken)
        put(SETUP_COMPLETED, settings.setupCompleted.toString())
        putPolicy("info", settings.policies.info)
        putPolicy("warning", settings.policies.warning)
        putPolicy("critical", settings.policies.critical)
    }

    fun decode(values: Map<String, String>): AppSettings {
        val defaults = AppSettings()
        val serverUrl = values[SERVER_URL]?.trim().takeUnless { it.isNullOrEmpty() } ?: defaults.serverUrl
        val webSocketUrl = values[WEBSOCKET_URL]?.trim().takeUnless { it.isNullOrEmpty() }
            ?: deriveWebSocketUrl(serverUrl)
        return AppSettings(
            serverUrl = serverUrl,
            deviceId = values[DEVICE_ID]?.trim().takeUnless { it.isNullOrEmpty() } ?: defaults.deviceId,
            webSocketUrl = webSocketUrl,
            authToken = values[AUTH_TOKEN].orEmpty(),
            setupCompleted = values.bool(SETUP_COMPLETED, defaults.setupCompleted),
            policies = AlertPolicies(
                info = readPolicy("info", AlertPolicies.Default.info, values),
                warning = readPolicy("warning", AlertPolicies.Default.warning, values),
                critical = readPolicy("critical", AlertPolicies.Default.critical, values),
            ),
        )
    }

    fun write(prefs: MutablePreferences, settings: AppSettings) {
        encode(settings).forEach { (key, value) ->
            prefs[stringPreferencesKey(key)] = value
        }
    }

    private fun MutableMap<String, String>.putPolicy(name: String, policy: SeverityPolicy) {
        put("${name}_sound", policy.soundName)
        put("${name}_volume", policy.volumePercent.toString())
        put("${name}_repeat", policy.repeatCount.toString())
        put("${name}_persistent", policy.persistent.toString())
        put("${name}_require_ack", policy.requireAck.toString())
    }

    private fun readPolicy(name: String, fallback: SeverityPolicy, values: Map<String, String>): SeverityPolicy {
        return SeverityPolicy(
            soundName = values["${name}_sound"]?.trim().takeUnless { it.isNullOrEmpty() } ?: fallback.soundName,
            volumePercent = values.int("${name}_volume", fallback.volumePercent).coerceIn(0, 100),
            repeatCount = values.int("${name}_repeat", fallback.repeatCount).coerceAtLeast(1),
            persistent = values.bool("${name}_persistent", fallback.persistent),
            requireAck = values.bool("${name}_require_ack", fallback.requireAck),
        )
    }

    private fun Map<String, String>.int(key: String, default: Int): Int = this[key]?.toIntOrNull() ?: default

    private fun Map<String, String>.bool(key: String, default: Boolean): Boolean = when (this[key]) {
        "true" -> true
        "false" -> false
        else -> default
    }
}
