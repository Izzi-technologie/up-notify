package com.wayscompany.webhookalarm.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

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

fun Context.openBatteryOptimizationSettings() {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:$packageName")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(intent) }.onFailure {
        val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(fallback)
    }
}

data class PhoneAlertReadiness(
    val notificationsGranted: Boolean,
    val overlayGranted: Boolean,
    val fullScreenIntentGranted: Boolean,
    val batteryExempt: Boolean,
) {
    val readyForBackgroundAlerts: Boolean =
        notificationsGranted && (overlayGranted || fullScreenIntentGranted)
}

fun Context.phoneAlertReadiness(): PhoneAlertReadiness {
    val notificationsGranted = if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    return PhoneAlertReadiness(
        notificationsGranted = notificationsGranted,
        overlayGranted = canDrawAlertOverlay(),
        fullScreenIntentGranted = canUseAlertFullScreenIntent(),
        batteryExempt = isExemptFromBatteryOptimizations(),
    )
}
