package com.wayscompany.webhookalarm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wayscompany.webhookalarm.alarm.AlertState
import com.wayscompany.webhookalarm.ui.AlertOverlay
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
import com.wayscompany.webhookalarm.ui.theme.IzziWebhookAlarmTheme

class AlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        val runtime = (application as WebhookAlarmApp).runtime
        setContent {
            val alert by runtime.alertState.collectAsStateWithLifecycle()
            IzziWebhookAlarmTheme {
                BackHandler(enabled = alert !is AlertState.Idle) {
                    runtime.dismiss()
                }
                LaunchedEffect(alert) {
                    if (alert is AlertState.Idle) finish()
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AlarmColors.Background),
                ) {
                    AlertOverlay(
                        state = alert,
                        onAcknowledge = runtime::acknowledge,
                        onDismiss = runtime::dismiss,
                    )
                }
            }
        }
    }

    companion object {
        fun open(context: Context) {
            val intent = Intent(context, AlertActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(intent)
        }
    }
}
