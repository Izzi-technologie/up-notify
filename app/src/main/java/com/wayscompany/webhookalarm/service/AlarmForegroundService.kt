package com.wayscompany.webhookalarm.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wayscompany.webhookalarm.MainActivity
import com.wayscompany.webhookalarm.R
import com.wayscompany.webhookalarm.WebhookAlarmApp
import com.wayscompany.webhookalarm.alarm.AlertState
import com.wayscompany.webhookalarm.model.Severity
import com.wayscompany.webhookalarm.utils.AndroidAlarmLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AlarmForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastCriticalId: String? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        acquireWakeLock()
        val runtime = runtime()
        startInForeground(runtime.isAudioActive.value, runtime.alertState.value)
        runtime.ensureStarted()
        serviceScope.launch {
            runtime.isAudioActive.collect { active ->
                startInForeground(active, runtime.alertState.value)
            }
        }
        serviceScope.launch {
            runtime.alertState.collect { alert ->
                startInForeground(runtime.isAudioActive.value, alert)
                val criticalId = (alert as? AlertState.Active)
                    ?.takeIf { it.event.severity == Severity.CRITICAL }
                    ?.event
                    ?.id
                if (criticalId != null && criticalId != lastCriticalId) {
                    lastCriticalId = criticalId
                    bringToFront()
                }
                if (criticalId == null) lastCriticalId = null
            }
        }
        bringToFront()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val runtime = runtime()
        startInForeground(runtime.isAudioActive.value, runtime.alertState.value)
        runtime.ensureStarted()
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        wakeLock?.let { lock ->
            if (lock.isHeld) lock.release()
        }
        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun runtime() = (application as WebhookAlarmApp).runtime

    private fun startInForeground(audioActive: Boolean, alert: AlertState) {
        val notification = buildNotification(alert)
        if (Build.VERSION.SDK_INT >= 34) {
            val special = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            val type = if (audioActive) {
                special or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                special
            }
            startForeground(NOTIFICATION_ID, notification, type)
        } else if (Build.VERSION.SDK_INT >= 29 && audioActive) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(alert: AlertState): Notification {
        val text = when (alert) {
            is AlertState.Active -> if (alert.event.severity == Severity.CRITICAL) {
                "Critical alert"
            } else {
                alert.event.title
            }
            is AlertState.Acknowledged -> "Alert acknowledged"
            is AlertState.Resolved -> "Alert resolved"
            AlertState.Idle -> "Alarm terminal running"
        }
        val launch = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(launch)
            .setFullScreenIntent(launch, true)
            .build()
    }

    private fun bringToFront() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        try {
            startActivity(intent)
        } catch (error: Throwable) {
            AndroidAlarmLogger.w("Unable to open the alarm screen: ${error.message}")
        }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        // Held for the life of the foreground service so the socket stays up on a dedicated TV.
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        val lock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WebhookAlarm:socket")
        lock.setReferenceCounted(false)
        lock.acquire()
        wakeLock = lock
    }

    companion object {
        private const val CHANNEL_ID = "webhook_alarm"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, AlarmForegroundService::class.java)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (error: Throwable) {
                AndroidAlarmLogger.e("Unable to start alarm service", error)
            }
        }
    }
}
