package com.wayscompany.webhookalarm.websocket

sealed interface ConnectionState {
    data object Offline : ConnectionState
    data object Connecting : ConnectionState
    data object Connected : ConnectionState
    data object Disconnected : ConnectionState
    data class Error(val message: String) : ConnectionState
}

object ReconnectBackoff {
    const val unexpectedCloseMs = 300L
    const val registerAckMs = 10_000L
    const val pingIntervalMs = 15_000L
    const val pongTimeoutMs = 25_000L

    private val stepsMs = longArrayOf(5_000L, 10_000L, 30_000L, 60_000L)

    fun delayMs(attempt: Int): Long = stepsMs[attempt.coerceIn(0, stepsMs.lastIndex)]
}
