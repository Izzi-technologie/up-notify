package com.wayscompany.webhookalarm.settings

import com.wayscompany.webhookalarm.model.Severity

data class SeverityPolicy(
    val soundName: String,
    val volumePercent: Int,
    val repeatCount: Int,
    val persistent: Boolean,
    val requireAck: Boolean,
)

data class AlertPolicies(
    val info: SeverityPolicy,
    val warning: SeverityPolicy,
    val critical: SeverityPolicy,
) {
    fun forSeverity(severity: Severity): SeverityPolicy = when (severity) {
        Severity.INFO -> info
        Severity.WARNING -> warning
        Severity.CRITICAL -> critical
    }

    fun withPolicy(severity: Severity, policy: SeverityPolicy): AlertPolicies = when (severity) {
        Severity.INFO -> copy(info = policy)
        Severity.WARNING -> copy(warning = policy)
        Severity.CRITICAL -> copy(critical = policy)
    }

    companion object {
        val Default = AlertPolicies(
            info = SeverityPolicy(
                soundName = "info",
                volumePercent = 40,
                repeatCount = 1,
                persistent = false,
                requireAck = false,
            ),
            warning = SeverityPolicy(
                soundName = "warning",
                volumePercent = 75,
                repeatCount = 3,
                persistent = false,
                requireAck = false,
            ),
            critical = SeverityPolicy(
                soundName = "critical",
                volumePercent = 100,
                repeatCount = 1,
                persistent = true,
                requireAck = true,
            ),
        )
    }
}

data class AppSettings(
    val serverUrl: String = "https://alarm.example.com",
    val deviceId: String = "tv-001",
    val webSocketUrl: String = "wss://alarm.example.com/ws",
    val authToken: String = "",
    val setupCompleted: Boolean = false,
    val policies: AlertPolicies = AlertPolicies.Default,
)
