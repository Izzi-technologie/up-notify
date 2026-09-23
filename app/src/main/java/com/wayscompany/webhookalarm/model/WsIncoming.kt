package com.wayscompany.webhookalarm.model

sealed interface WsIncoming {
    data class Connected(val deviceId: String) : WsIncoming
    data class Alert(val event: AlertEvent) : WsIncoming
    data class Resolved(val id: String) : WsIncoming
}
