package com.wayscompany.webhookalarm.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wayscompany.webhookalarm.service.AlarmForegroundService
import com.wayscompany.webhookalarm.utils.AndroidAlarmLogger

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in BOOT_ACTIONS) return
        AndroidAlarmLogger.i("Boot completed, starting alarm service")
        AlarmForegroundService.start(context)
    }

    private companion object {
        val BOOT_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
        )
    }
}
