package com.wayscompany.webhookalarm.alarm

import com.wayscompany.webhookalarm.model.AlertEvent
import com.wayscompany.webhookalarm.model.Severity
import com.wayscompany.webhookalarm.settings.SeverityPolicy

sealed interface AlertState {
    data object Idle : AlertState
    data class Active(val event: AlertEvent, val policy: SeverityPolicy) : AlertState
    data class Acknowledged(val event: AlertEvent) : AlertState
    data class Resolved(val event: AlertEvent) : AlertState
}

enum class AlertPresentation {
    BANNER,
    OVERLAY,
    FULLSCREEN,
}

fun presentationFor(severity: Severity): AlertPresentation = when (severity) {
    Severity.INFO -> AlertPresentation.BANNER
    Severity.WARNING -> AlertPresentation.OVERLAY
    Severity.CRITICAL -> AlertPresentation.FULLSCREEN
}
