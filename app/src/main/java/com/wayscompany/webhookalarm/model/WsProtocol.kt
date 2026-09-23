package com.wayscompany.webhookalarm.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RegisterMessage(
    val type: String = "register",
    val deviceId: String,
)

@Serializable
data class AcknowledgeMessage(
    val type: String = "acknowledge",
    val alertId: String,
    val deviceId: String,
)

object WsProtocol {
    private val json = Json { encodeDefaults = true }

    fun register(deviceId: String): String =
        json.encodeToString(RegisterMessage(deviceId = deviceId))

    fun acknowledge(alertId: String, deviceId: String): String =
        json.encodeToString(AcknowledgeMessage(alertId = alertId, deviceId = deviceId))
}
