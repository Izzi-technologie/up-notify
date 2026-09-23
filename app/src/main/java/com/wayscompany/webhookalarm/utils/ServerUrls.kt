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
