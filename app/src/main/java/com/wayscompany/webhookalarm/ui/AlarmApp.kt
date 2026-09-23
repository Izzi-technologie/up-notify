package com.wayscompany.webhookalarm.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wayscompany.webhookalarm.alarm.AlertState
import com.wayscompany.webhookalarm.model.Severity
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
import com.wayscompany.webhookalarm.ui.theme.WebhookAlarmTheme

@Composable
fun AlarmApp(viewModel: MainViewModel) {
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val alert by viewModel.alert.collectAsStateWithLifecycle()
    val lastEvent by viewModel.lastEvent.collectAsStateWithLifecycle()
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val alertBlocksBack = when (val current = alert) {
        is AlertState.Active -> current.event.severity == Severity.CRITICAL
        is AlertState.Acknowledged -> true
        else -> false
    }

    WebhookAlarmTheme {
        BackHandler(enabled = screen == AppScreen.Settings && !alertBlocksBack) {
            viewModel.closeSettings()
        }
        BackHandler(enabled = alertBlocksBack) {}
        Box(modifier = Modifier.fillMaxSize().background(AlarmColors.Background)) {
            if (!loaded) {
                Box(modifier = Modifier.fillMaxSize().background(AlarmColors.Background))
            } else if (!settings.setupCompleted) {
                SetupScreen(
                    connection = connection,
                    initialServerUrl = settings.serverUrl,
                    initialDeviceId = settings.deviceId,
                    onConnect = viewModel::connect,
                    onContinue = viewModel::continueSetup,
                )
            } else when (screen) {
                AppScreen.Settings -> SettingsScreen(
                    settings = settings,
                    onSave = viewModel::save,
                    onTestConnection = viewModel::testConnection,
                    onTestCritical = viewModel::testCritical,
                    onRestart = viewModel::restartService,
                )
                AppScreen.Home -> HomeScreen(
                    connection = connection,
                    deviceId = settings.deviceId,
                    serverUrl = settings.webSocketUrl,
                    lastEvent = lastEvent,
                    onTest = viewModel::test,
                    onSettings = viewModel::openSettings,
                )
            }
            AlertOverlay(state = alert, onAcknowledge = viewModel::acknowledge)
        }
    }
}
