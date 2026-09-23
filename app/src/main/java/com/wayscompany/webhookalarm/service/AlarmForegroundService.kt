package com.wayscompany.webhookalarm.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.wayscompany.webhookalarm.MainActivity
import com.wayscompany.webhookalarm.R
import com.wayscompany.webhookalarm.WebhookAlarmApp
import com.wayscompany.webhookalarm.alarm.AlertState
import com.wayscompany.webhookalarm.overlay.SystemAlertOverlay
import com.wayscompany.webhookalarm.utils.AndroidAlarmLogger
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AlarmForegroundService : LifecycleService(), SavedStateRegistryOwner {
    private val savedStateController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var notifications: AlertNotificationController
    private lateinit var presenter: BackgroundAlertPresenter
    private lateinit var overlay: SystemAlertOverlay

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        val runtime = runtime()
        notifications = AlertNotificationController(this)
        overlay = SystemAlertOverlay(
            context = this,
            runtime = runtime,
            lifecycleOwner = this,
            savedStateOwner = this,
        )
        presenter = BackgroundAlertPresenter(
            context = this,
            runtime = runtime,
            notifications = notifications,
            overlay = overlay,
        )
        acquireWakeLock()
        startInForeground(runtime.isAudioActive.value)
        runtime.ensureStarted()
        presenter.start(serviceScope)
        serviceScope.launchCollectors(runtime)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val runtime = runtime()
        startInForeground(runtime.isAudioActive.value)
        runtime.ensureStarted()
        return Service.START_STICKY
    }

    override fun onDestroy() {
        overlay.hide()
        serviceScope.cancel()
        wakeLock?.let { lock ->
            if (lock.isHeld) lock.release()
        }
        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    private fun runtime() = (application as WebhookAlarmApp).runtime

    private fun CoroutineScope.launchCollectors(runtime: com.wayscompany.webhookalarm.AppRuntime) {
        launch {
            runtime.isAudioActive.collect { active -> startInForeground(active) }
        }
        launch {
            runtime.alertState.collect { alert -> updateServiceNotification(alert) }
        }
    }

    private fun startInForeground(audioActive: Boolean) {
        val notification = buildServiceNotification()
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

    private fun updateServiceNotification(alert: AlertState) {
        val text = when (alert) {
            is AlertState.Active -> alert.event.title.ifBlank { "Alert active" }
            is AlertState.Acknowledged -> "Alert acknowledged"
            is AlertState.Resolved -> "Alert resolved"
            AlertState.Idle -> getString(R.string.notification_service_running)
        }
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildServiceNotification(text))
    }

    private fun buildServiceNotification(contentText: String = getString(R.string.notification_service_running)): Notification {
        val launch = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, AlertNotificationController.SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(launch)
            .build()
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        val lock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "IzziWebhookAlarm:socket")
        lock.setReferenceCounted(false)
        lock.acquire()
        wakeLock = lock
    }

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, AlarmForegroundService::class.java)
            try {
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } catch (error: Throwable) {
                AndroidAlarmLogger.e("Unable to start alarm service", error)
            }
        }
    }
}
