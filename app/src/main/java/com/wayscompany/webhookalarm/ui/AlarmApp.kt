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
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
import com.wayscompany.webhookalarm.ui.theme.IzziWebhookAlarmTheme

@Composable
fun AlarmApp(
    viewModel: MainViewModel,
    onRequestNotifications: () -> Unit,
) {
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val alert by viewModel.alert.collectAsStateWithLifecycle()
    val lastEvent by viewModel.lastEvent.collectAsStateWithLifecycle()
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val alertVisible = alert !is AlertState.Idle

    IzziWebhookAlarmTheme {
        BackHandler(enabled = screen == AppScreen.Settings && !alertVisible) {
            viewModel.closeSettings()
        }
        BackHandler(enabled = alertVisible) {
            viewModel.dismiss()
        }
        Box(modifier = Modifier.fillMaxSize().background(AlarmColors.Background)) {
            if (!loaded) {
                Box(modifier = Modifier.fillMaxSize().background(AlarmColors.Background))
            } else if (!settings.setupCompleted) {
                SetupScreen(
                    connection = connection,
                    initialServerUrl = settings.serverUrl,
                    initialDeviceId = settings.deviceId,
                    initialAuthToken = settings.authToken,
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
                    serverUrl = settings.serverUrl,
                    lastEvent = lastEvent,
                    onTest = viewModel::test,
                    onSettings = viewModel::openSettings,
                    onRequestNotifications = onRequestNotifications,
                )
            }
            AlertOverlay(
                state = alert,
                onAcknowledge = viewModel::acknowledge,
                onDismiss = viewModel::dismiss,
            )
        }
    }
}
