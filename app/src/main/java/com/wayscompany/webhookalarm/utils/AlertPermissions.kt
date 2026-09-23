package com.wayscompany.webhookalarm.utils

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

private const val ACTION_VIEW_ADVANCED_POWER_USAGE_DETAIL =
    "android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL"

fun Context.canDrawAlertOverlay(): Boolean = Settings.canDrawOverlays(this)

fun Context.canUseAlertFullScreenIntent(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
    val manager = ContextCompat.getSystemService(this, android.app.NotificationManager::class.java)
        ?: return false
    return manager.canUseFullScreenIntent()
}

fun Context.isExemptFromBatteryOptimizations(): Boolean {
    val power = getSystemService(Context.POWER_SERVICE) as PowerManager
    return power.isIgnoringBatteryOptimizations(packageName)
}

fun Context.openOverlayPermissionSettings() {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        "package:$packageName".toUri(),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

fun Context.openFullScreenIntentSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
        data = "package:$packageName".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

fun Context.openNotificationSettings() {
    val packageUri = Uri.fromParts("package", packageName, null)
    startFirstSettingsIntent(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra("app_package", packageName)
            putExtra("app_uid", applicationInfo.uid)
        },
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = packageUri
        },
    )
}

fun Context.openBatteryOptimizationSettings() {
    val packageUri = Uri.fromParts("package", packageName, null)
    startFirstSettingsIntent(
        Intent(ACTION_VIEW_ADVANCED_POWER_USAGE_DETAIL).apply {
            data = packageUri
            putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
        },
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = packageUri
        },
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = packageUri
        },
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
    )
}

private fun Context.startFirstSettingsIntent(vararg intents: Intent) {
    for (intent in intents) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { startActivity(intent) }.isSuccess) return
    }
    AndroidAlarmLogger.w("Unable to open settings for ${intents.firstOrNull()?.action}")
}

data class PhoneAlertReadiness(
    val notificationsGranted: Boolean,
    val overlayGranted: Boolean,
    val fullScreenIntentGranted: Boolean,
    val batteryExempt: Boolean,
) {
    val readyForBackgroundAlerts: Boolean =
        notificationsGranted && (overlayGranted || fullScreenIntentGranted)

    val allGranted: Boolean =
        notificationsGranted && overlayGranted && fullScreenIntentGranted && batteryExempt
}

fun Context.phoneAlertReadiness(): PhoneAlertReadiness {
    val notificationsGranted = ContextCompat.getSystemService(this, NotificationManager::class.java)
        ?.areNotificationsEnabled() == true
    return PhoneAlertReadiness(
        notificationsGranted = notificationsGranted,
        overlayGranted = canDrawAlertOverlay(),
        fullScreenIntentGranted = canUseAlertFullScreenIntent(),
        batteryExempt = isExemptFromBatteryOptimizations(),
    )
}
