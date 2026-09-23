package com.wayscompany.webhookalarm.alarm

import com.wayscompany.webhookalarm.model.AlertEvent
import com.wayscompany.webhookalarm.model.Severity
import com.wayscompany.webhookalarm.settings.AlertPolicies
import com.wayscompany.webhookalarm.settings.SeverityPolicy
import com.wayscompany.webhookalarm.utils.AlarmLogger
import com.wayscompany.webhookalarm.utils.NoOpLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlertEngine(
    private val audio: AlarmAudio,
    private val scope: CoroutineScope,
    private val policies: () -> AlertPolicies,
    private val onAcknowledge: (String) -> Unit,
    private val logger: AlarmLogger = NoOpLogger,
    private val resolvedHoldMs: Long = 3_000L,
    private val idFactory: () -> String = { "test-${System.currentTimeMillis()}" },
    private val clock: () -> String = { java.time.Instant.now().toString() },
) {
    private val _state = MutableStateFlow<AlertState>(AlertState.Idle)
    val state: StateFlow<AlertState> = _state.asStateFlow()

    private val _lastEvent = MutableStateFlow<AlertEvent?>(null)
    val lastEvent: StateFlow<AlertEvent?> = _lastEvent.asStateFlow()

    private var resolveJob: Job? = null

    fun onAlert(event: AlertEvent) {
        logger.i("Alert received")
        _lastEvent.value = event
        val activeSeverity = severityOf(_state.value)
        if (activeSeverity != null && event.severity.rank < activeSeverity.rank) {
            logger.i("Alert ignored because a higher severity is active")
            return
        }
        resolveJob?.cancel()
        val policy = policies().forSeverity(event.severity)
        audio.stop()
        _state.value = AlertState.Active(event, policy)
        if (event.severity == Severity.CRITICAL) {
            logger.i("Critical alert started")
        }
        audio.play(policy.toPlayback()) {
            scope.launch { onPlaybackFinished(event.id) }
        }
    }

    fun onResolved(id: String) {
        val event = eventOf(_state.value) ?: return
        if (event.id != id) return
        resolveJob?.cancel()
        audio.stop()
        _state.value = AlertState.Resolved(event)
        logger.i("Alert resolved")
        resolveJob = scope.launch {
            delay(resolvedHoldMs)
            val current = _state.value
            if (current is AlertState.Resolved && current.event.id == id) {
                _state.value = AlertState.Idle
            }
        }
    }

    fun acknowledge(): String? {
        val current = _state.value as? AlertState.Active ?: return null
        if (!current.policy.requireAck) return null
        audio.stop()
        _state.value = AlertState.Acknowledged(current.event)
        logger.i("Critical alert acknowledged")
        onAcknowledge(current.event.id)
        return current.event.id
    }

    fun dismiss() {
        when (val current = _state.value) {
            AlertState.Idle -> return
            is AlertState.Resolved -> {
                resolveJob?.cancel()
                _state.value = AlertState.Idle
            }
            is AlertState.Acknowledged -> {
                audio.stop()
                _state.value = AlertState.Idle
                logger.i("Alert dismissed")
            }
            is AlertState.Active -> {
                resolveJob?.cancel()
                audio.stop()
                if (current.policy.requireAck) {
                    logger.i("Critical alert acknowledged")
                    onAcknowledge(current.event.id)
                }
                _state.value = AlertState.Idle
                logger.i("Alert dismissed")
            }
        }
    }

    fun test(severity: Severity) {
        val label = severity.name.lowercase().replaceFirstChar { it.uppercase() }
        onAlert(
            AlertEvent(
                id = idFactory(),
                type = "alert",
                severity = severity,
                title = "Test $label",
                message = "Test ${severity.name.lowercase()} alert",
                timestamp = clock(),
            ),
        )
    }

    private fun onPlaybackFinished(alertId: String) {
        val current = _state.value as? AlertState.Active ?: return
        if (current.event.id != alertId) return
        if (current.policy.requireAck || current.policy.persistent) return
        _state.value = AlertState.Idle
    }
}

private fun SeverityPolicy.toPlayback(): PlaybackRequest = PlaybackRequest(
    soundName = soundName,
    volumePercent = volumePercent,
    loop = persistent,
    repeatCount = repeatCount.coerceAtLeast(1),
)

private fun severityOf(state: AlertState): Severity? = when (state) {
    is AlertState.Active -> state.event.severity
    is AlertState.Acknowledged -> state.event.severity
    is AlertState.Resolved -> null
    AlertState.Idle -> null
}

private fun eventOf(state: AlertState): AlertEvent? = when (state) {
    is AlertState.Active -> state.event
    is AlertState.Acknowledged -> state.event
    is AlertState.Resolved -> state.event
    AlertState.Idle -> null
}
