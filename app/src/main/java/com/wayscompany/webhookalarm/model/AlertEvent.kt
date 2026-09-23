package com.wayscompany.webhookalarm.model

data class AlertEvent(
    val id: String,
    val type: String,
    val severity: Severity,
    val title: String,
    val message: String,
    val timestamp: String?,
)
