package com.wayscompany.webhookalarm.data

import com.wayscompany.webhookalarm.model.AlertEvent
import com.wayscompany.webhookalarm.model.Severity
import com.wayscompany.webhookalarm.model.WsIncoming
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object AlertParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(
        raw: String,
        idFactory: () -> String = { "evt-${System.currentTimeMillis()}" },
    ): WsIncoming? {
        val element = try {
            json.parseToJsonElement(raw)
        } catch (_: Exception) {
            return null
        }
        val obj = element as? JsonObject ?: return null
        return when (obj.string("type")) {
            "connected" -> WsIncoming.Connected(obj.string("deviceId").orEmpty())
            "alert" -> WsIncoming.Alert(obj.toAlert(idFactory))
            "alert_resolved" -> {
                val id = obj.string("id") ?: return null
                WsIncoming.Resolved(id)
            }
            else -> null
        }
    }

    private fun JsonObject.toAlert(idFactory: () -> String): AlertEvent {
        val monitorName = (this["monitor"] as? JsonObject)?.string("name")
        val message = string("message") ?: string("text") ?: ""
        val title = string("title") ?: monitorName ?: message.ifBlank { "Alert" }
        return AlertEvent(
            id = string("id") ?: idFactory(),
            type = string("type") ?: "alert",
            severity = Severity.fromRaw(string("severity")),
            title = title,
            message = message,
            timestamp = string("timestamp"),
        )
    }

    private fun JsonObject.string(key: String): String? {
        val primitive = this[key] as? JsonPrimitive ?: return null
        if (primitive is JsonNull) return null
        return primitive.contentOrNull?.takeIf { it.isNotBlank() }
    }
}
