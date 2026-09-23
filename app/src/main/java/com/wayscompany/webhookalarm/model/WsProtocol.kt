package com.wayscompany.webhookalarm.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

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

@Serializable
data class PingMessage(
    val type: String = "ping",
)

object WsProtocol {
    private val json = Json { encodeDefaults = true }

    fun register(deviceId: String): String =
        json.encodeToString(RegisterMessage(deviceId = deviceId))

    fun acknowledge(alertId: String, deviceId: String): String =
        json.encodeToString(AcknowledgeMessage(alertId = alertId, deviceId = deviceId))

    fun ping(): String = json.encodeToString(PingMessage())

    fun isPong(text: String): Boolean = try {
        val type = ((json.parseToJsonElement(text) as? JsonObject)?.get("type") as? JsonPrimitive)
            ?.contentOrNull
        type == "pong"
    } catch (_: Exception) {
        false
    }
}
