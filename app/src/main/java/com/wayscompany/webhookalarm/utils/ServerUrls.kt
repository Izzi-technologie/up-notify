package com.wayscompany.webhookalarm.utils

fun deriveWebSocketUrl(serverUrl: String): String {
    val trimmed = serverUrl.trim().trimEnd('/')
    if (trimmed.isEmpty()) return ""
    val withScheme = when {
        trimmed.startsWith("wss://") || trimmed.startsWith("ws://") -> trimmed
        trimmed.startsWith("https://") -> "wss://${trimmed.removePrefix("https://")}"
        trimmed.startsWith("http://") -> "ws://${trimmed.removePrefix("http://")}"
        else -> "wss://$trimmed"
    }
    return if (withScheme.endsWith("/ws")) withScheme else "$withScheme/ws"
}

fun deriveWebhookUrl(serverUrl: String, deviceId: String): String {
    val id = deviceId.trim()
    val base = normalizeHttpBaseUrl(serverUrl)
    if (id.isEmpty() || base.isEmpty()) return ""
    return "$base/webhook/$id"
}

fun normalizeHttpBaseUrl(serverUrl: String): String {
    val trimmed = serverUrl.trim().trimEnd('/')
    if (trimmed.isEmpty()) return ""
    val withoutWsPath = trimmed.substringBefore("/ws").trimEnd('/')
    return when {
        withoutWsPath.startsWith("wss://") ->
            "https://${withoutWsPath.removePrefix("wss://")}"
        withoutWsPath.startsWith("ws://") ->
            "http://${withoutWsPath.removePrefix("ws://")}"
        withoutWsPath.startsWith("https://") || withoutWsPath.startsWith("http://") ->
            withoutWsPath
        else -> "https://$withoutWsPath"
    }
}

fun normalizeWebSocketUrl(raw: String): String? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    return when {
        value.startsWith("wss://") || value.startsWith("ws://") -> value
        value.startsWith("https://") || value.startsWith("http://") -> deriveWebSocketUrl(value)
        else -> null
    }
}

fun displayTime(timestamp: String?): String? {
    if (timestamp.isNullOrBlank()) return null
    val time = timestamp.substringAfter('T', "")
    if (time.isEmpty()) return timestamp
    return time.substringBefore('Z').substringBefore('+').substringBefore('.')
}
