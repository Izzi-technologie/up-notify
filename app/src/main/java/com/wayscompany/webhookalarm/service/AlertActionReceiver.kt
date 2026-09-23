package com.wayscompany.webhookalarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wayscompany.webhookalarm.AlertActivity
import com.wayscompany.webhookalarm.WebhookAlarmApp

class AlertActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val runtime = (context.applicationContext as WebhookAlarmApp).runtime
        when (action) {
            ACTION_ACKNOWLEDGE -> runtime.acknowledge()
            ACTION_DISMISS -> runtime.dismiss()
            ACTION_OPEN -> AlertActivity.open(context)
        }
    }

    companion object {
        const val ACTION_ACKNOWLEDGE = "com.wayscompany.webhookalarm.action.ACKNOWLEDGE"
        const val ACTION_DISMISS = "com.wayscompany.webhookalarm.action.DISMISS"
        const val ACTION_OPEN = "com.wayscompany.webhookalarm.action.OPEN_ALERT"
    }
}
