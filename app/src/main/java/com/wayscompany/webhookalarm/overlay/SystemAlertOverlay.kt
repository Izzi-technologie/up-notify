package com.wayscompany.webhookalarm.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.wayscompany.webhookalarm.AppRuntime
import com.wayscompany.webhookalarm.alarm.AlertState
import com.wayscompany.webhookalarm.model.Severity
import com.wayscompany.webhookalarm.ui.AlertOverlay
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
import com.wayscompany.webhookalarm.ui.theme.IzziWebhookAlarmTheme
import com.wayscompany.webhookalarm.utils.canDrawAlertOverlay
import com.wayscompany.webhookalarm.utils.isTelevisionDevice

class SystemAlertOverlay(
    context: Context,
    private val runtime: AppRuntime,
    private val lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    private val savedStateOwner: androidx.savedstate.SavedStateRegistryOwner,
) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var shown = false

    fun sync(alert: AlertState, appInForeground: Boolean) {
        if (appContext.isTelevisionDevice() || !appContext.canDrawAlertOverlay()) {
            hide()
            return
        }
        if (appInForeground || !shouldOverlay(alert)) {
            hide()
            return
        }
        show()
    }

    fun hide() {
        if (!shown) return
        val view = composeView ?: return
        runCatching { windowManager.removeView(view) }
        composeView = null
        shown = false
    }

    private fun show() {
        if (shown) return
        val view = ComposeView(appContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(savedStateOwner)
            setContent {
                val alert by runtime.alertState.collectAsStateWithLifecycle()
                IzziWebhookAlarmTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AlarmColors.Background.copy(alpha = 0.02f)),
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
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT,
        )
        windowManager.addView(view, params)
        composeView = view
        shown = true
    }

    private fun shouldOverlay(alert: AlertState): Boolean = when (alert) {
        AlertState.Idle -> false
        is AlertState.Active -> alert.event.severity != Severity.INFO
        is AlertState.Acknowledged -> true
        is AlertState.Resolved -> true
    }
}
