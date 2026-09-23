package com.wayscompany.webhookalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlarmColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth(),
            contentPadding = PaddingValues(48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Text(text = "WEBHOOK ALARM", color = AlarmColors.Text, fontSize = 48.sp)
            }
            item {
                Text(text = "Configure your TV", color = AlarmColors.Muted, fontSize = 28.sp)
            }
            item {
                TvTextField(label = "Server URL", value = serverUrl, onValueChange = { serverUrl = it })
            }
            item {
                TvTextField(label = "Device ID", value = deviceId, onValueChange = { deviceId = it })
            }
            item {
                TvTextField(label = "Token (optional)", value = authToken, onValueChange = { authToken = it })
            }
            item {
                Text(
                    text = deriveWebSocketUrl(serverUrl).ifBlank { "WebSocket URL" },
                    color = AlarmColors.Muted,
                    fontSize = 20.sp,
                )
            }
            item {
                ConnectionLine(connection)
            }
            if (error != null) {
                item {
                    Text(text = error.orEmpty(), color = AlarmColors.Critical, fontSize = 20.sp)
                }
            }
            if (connection is ConnectionState.Error) {
                item {
                    Text(text = connection.message, color = AlarmColors.Critical, fontSize = 20.sp)
                }
            }
            item {
                TvButton(text = "CONNECT", onClick = {
                    if (serverUrl.isBlank() || deviceId.isBlank()) {
                        error = "Server URL and Device ID are required"
                    } else {
                        error = null
                        onConnect(serverUrl, deviceId, authToken)
                    }
                })
            }
            if (connection is ConnectionState.Connected) {
                item {
                    Text(text = "Connected", color = AlarmColors.Connected, fontSize = 28.sp)
                }
                item {
                    TvButton(text = "CONTINUE", onClick = onContinue)
                }
            }
        }
    }
}
