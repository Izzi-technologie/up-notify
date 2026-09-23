package com.wayscompany.webhookalarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.wayscompany.webhookalarm.AlertActivity
import com.wayscompany.webhookalarm.R
import com.wayscompany.webhookalarm.alarm.AlertState
import com.wayscompany.webhookalarm.model.Severity
import com.wayscompany.webhookalarm.utils.canUseAlertFullScreenIntent

class AlertNotificationController(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        createChannels()
    }

    fun show(alert: AlertState) {
        if (alert is AlertState.Idle) {
            manager.cancel(ALERT_NOTIFICATION_ID)
            return
        }
        manager.notify(ALERT_NOTIFICATION_ID, buildAlertNotification(alert))
    }

    private fun buildAlertNotification(alert: AlertState): Notification {
        val (title, text, severity) = contentFor(alert)
        val openIntent = activityPendingIntent(AlertActionReceiver.ACTION_OPEN, REQUEST_OPEN)
        val fullScreen = if (shouldUseFullScreen(severity)) openIntent else null
        val builder = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openIntent)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
        if (fullScreen != null && context.canUseAlertFullScreenIntent()) {
            builder.setFullScreenIntent(fullScreen, true)
        }
        when (severity) {
            Severity.CRITICAL -> {
                builder.setOngoing(true)
                builder.setColorized(true)
                if (alert is AlertState.Active && alert.policy.requireAck) {
                    builder.addAction(
                        R.drawable.ic_notification,
                        context.getString(R.string.notification_action_ack),
                        broadcastPendingIntent(AlertActionReceiver.ACTION_ACKNOWLEDGE, REQUEST_ACK),
                    )
                }
            }
            Severity.WARNING -> builder.setOngoing(true)
            Severity.INFO -> builder.setTimeoutAfter(30_000)
            null -> Unit
        }
        builder.addAction(
            R.drawable.ic_notification,
            context.getString(R.string.notification_action_dismiss),
            broadcastPendingIntent(AlertActionReceiver.ACTION_DISMISS, REQUEST_DISMISS),
        )
        return builder.build()
    }

    private fun contentFor(alert: AlertState): Triple<String, String, Severity?> = when (alert) {
        is AlertState.Active -> Triple(
            labelFor(alert.event.severity),
            alert.event.title.ifBlank { alert.event.message },
            alert.event.severity,
        )
        is AlertState.Acknowledged -> Triple(
            context.getString(R.string.notification_title_acknowledged),
            alert.event.title,
            alert.event.severity,
        )
        is AlertState.Resolved -> Triple(
            context.getString(R.string.notification_title_resolved),
            alert.event.title,
            null,
        )
        AlertState.Idle -> Triple("", "", null)
    }

    private fun labelFor(severity: Severity): String = when (severity) {
        Severity.INFO -> context.getString(R.string.notification_title_info)
        Severity.WARNING -> context.getString(R.string.notification_title_warning)
        Severity.CRITICAL -> context.getString(R.string.notification_title_critical)
    }

    private fun shouldUseFullScreen(severity: Severity?): Boolean =
        severity == Severity.CRITICAL || severity == Severity.WARNING

    private fun activityPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, AlertActivity::class.java).apply {
            this.action = action
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun broadcastPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(action).setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val alerts = NotificationChannel(
            ALERT_CHANNEL_ID,
            context.getString(R.string.notification_alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_alert_channel_description)
            enableVibration(true)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val service = NotificationChannel(
            SERVICE_CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(alerts)
        manager.createNotificationChannel(service)
    }

    companion object {
        const val SERVICE_CHANNEL_ID = "webhook_alarm_service"
        const val ALERT_CHANNEL_ID = "webhook_alarm_alerts"
        const val ALERT_NOTIFICATION_ID = 1002

        private const val REQUEST_OPEN = 2001
        private const val REQUEST_ACK = 2002
        private const val REQUEST_DISMISS = 2003
    }
}
