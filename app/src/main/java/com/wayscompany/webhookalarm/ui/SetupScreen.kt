package com.wayscompany.webhookalarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import com.wayscompany.webhookalarm.ui.components.ScreenFooter
import com.wayscompany.webhookalarm.ui.components.ScreenHeader
import com.wayscompany.webhookalarm.ui.components.ScreenShell
import com.wayscompany.webhookalarm.ui.components.StatusBadge
import com.wayscompany.webhookalarm.ui.components.TvPanel
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
import com.wayscompany.webhookalarm.ui.theme.AlarmDimens
import com.wayscompany.webhookalarm.ui.theme.AlarmTypography
import com.wayscompany.webhookalarm.utils.deriveWebSocketUrl
import com.wayscompany.webhookalarm.websocket.ConnectionState

@Composable
fun SetupScreen(
    connection: ConnectionState,
    initialServerUrl: String,
    initialDeviceId: String,
    initialAuthToken: String,
    onConnect: (serverUrl: String, deviceId: String, authToken: String) -> Unit,
    onContinue: () -> Unit,
) {
    var serverUrl by rememberSaveable { mutableStateOf(initialServerUrl) }
    var deviceId by rememberSaveable { mutableStateOf(initialDeviceId) }
    var authToken by rememberSaveable { mutableStateOf(initialAuthToken) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val connected = connection is ConnectionState.Connected
    val messages = listOfNotNull(
        error,
        (connection as? ConnectionState.Error)?.message,
    )
    ScreenShell {
        item {
            ScreenHeader(
                title = "IZZI WEBHOOK ALARM",
                subtitle = "Connect this device to your alarm server",
                trailing = { StatusBadge(connection) },
                supporting = { ScreenFooter() },
            )
        }
        item {
            TvPanel {
                TvTextField(
                    label = "Server URL",
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                )
                TvTextField(
                    label = "Device ID",
                    value = deviceId,
                    onValueChange = { deviceId = it },
                )
                TvTextField(
                    label = "Token (optional)",
                    value = authToken,
                    onValueChange = { authToken = it },
                )
            }
        }
        item {
            Text(
                text = deriveWebSocketUrl(serverUrl).ifBlank { "WebSocket URL" },
                modifier = Modifier.padding(start = AlarmDimens.cardPadding),
                color = AlarmColors.Muted,
                style = AlarmTypography.caption,
            )
        }
        if (messages.isNotEmpty()) {
            item {
                TvPanel(
                    containerColor = AlarmColors.CriticalBackground,
                    borderColor = AlarmColors.Critical,
                ) {
                    messages.forEach { message ->
                        Text(
                            text = message,
                            color = AlarmColors.Text,
                            style = AlarmTypography.body,
                        )
                    }
                }
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AlarmDimens.itemGap),
            ) {
                TvButton(
                    text = "CONNECT",
                    onClick = {
                        if (serverUrl.isBlank() || deviceId.isBlank()) {
                            error = "Server URL and Device ID are required"
                        } else {
                            error = null
                            onConnect(serverUrl, deviceId, authToken)
                        }
                    },
                    variant = if (connected) TvButtonVariant.Secondary else TvButtonVariant.Primary,
                )
                if (connected) {
                    TvButton(
                        text = "CONTINUE",
                        onClick = onContinue,
                        variant = TvButtonVariant.Primary,
                    )
                }
            }
        }
    }
}
