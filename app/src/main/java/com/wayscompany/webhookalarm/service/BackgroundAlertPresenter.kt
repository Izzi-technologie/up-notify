package com.wayscompany.webhookalarm.service

import android.content.Context
import android.content.Intent
import com.wayscompany.webhookalarm.AlertActivity
import com.wayscompany.webhookalarm.AppRuntime
import com.wayscompany.webhookalarm.MainActivity
import com.wayscompany.webhookalarm.alarm.AlertState
import com.wayscompany.webhookalarm.model.Severity
import com.wayscompany.webhookalarm.overlay.SystemAlertOverlay
import com.wayscompany.webhookalarm.utils.AppForeground
import com.wayscompany.webhookalarm.utils.isTelevisionDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class BackgroundAlertPresenter(
    context: Context,
    private val runtime: AppRuntime,
    private val notifications: AlertNotificationController,
    private val overlay: SystemAlertOverlay,
) {
    private val appContext = context.applicationContext
    private val isTelevision = appContext.isTelevisionDevice()
    private var lastRaisedKey: String? = null

    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(runtime.alertState, AppForeground.inForeground) { alert, inForeground ->
                alert to inForeground
            }.collect { (alert, inForeground) ->
                present(alert, inForeground)
            }
        }
    }

    private fun present(alert: AlertState, inForeground: Boolean) {
        notifications.show(alert)
        overlay.sync(alert, inForeground)
        when {
            alert is AlertState.Idle -> lastRaisedKey = null
            isTelevision -> raiseTelevisionUi()
            !inForeground -> raisePhoneUi(alert)
        }
    }

    private fun raiseTelevisionUi() {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { appContext.startActivity(intent) }
    }

    private fun raisePhoneUi(alert: AlertState) {
        val key = presentationKey(alert) ?: return
        if (key == lastRaisedKey) return
        if (!shouldLaunchActivity(alert)) return
        lastRaisedKey = key
        AlertActivity.open(appContext)
    }

    private fun shouldLaunchActivity(alert: AlertState): Boolean = when (alert) {
        is AlertState.Active -> alert.event.severity != Severity.INFO
        is AlertState.Acknowledged -> true
        is AlertState.Resolved -> true
        AlertState.Idle -> false
    }

    private fun presentationKey(alert: AlertState): String? = when (alert) {
        is AlertState.Active -> "active:${alert.event.id}:${alert.event.severity}"
        is AlertState.Acknowledged -> "ack:${alert.event.id}"
        is AlertState.Resolved -> "resolved:${alert.event.id}"
        AlertState.Idle -> null
    }
}
