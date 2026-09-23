package com.wayscompany.webhookalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.wayscompany.webhookalarm.model.AlertEvent
import com.wayscompany.webhookalarm.model.Severity
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
import com.wayscompany.webhookalarm.utils.displayTime
import com.wayscompany.webhookalarm.websocket.ConnectionState

@Composable
fun HomeScreen(
    connection: ConnectionState,
    deviceId: String,
    serverUrl: String,
    lastEvent: AlertEvent?,
    onTest: (Severity) -> Unit,
    onSettings: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlarmColors.Background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 920.dp),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Text(text = "WEBHOOK ALARM", color = AlarmColors.Text, fontSize = 52.sp)
            }
            item { ConnectionLine(connection) }
            item { InfoLine(label = "Device", value = deviceId) }
            item { InfoLine(label = "Server", value = serverUrl) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Last event", color = AlarmColors.Muted, fontSize = 18.sp)
                    Text(
                        text = lastEvent?.message?.ifBlank { lastEvent.title } ?: "None",
                        color = AlarmColors.Text,
                        fontSize = 28.sp,
                    )
                    val time = displayTime(lastEvent?.timestamp)
                    if (time != null) {
                        Text(text = time, color = AlarmColors.Muted, fontSize = 22.sp)
                    }
                }
            }
            item { TvButton(text = "TEST INFO", onClick = { onTest(Severity.INFO) }) }
            item { TvButton(text = "TEST WARNING", onClick = { onTest(Severity.WARNING) }) }
            item { TvButton(text = "TEST CRITICAL", onClick = { onTest(Severity.CRITICAL) }) }
            item { TvButton(text = "SETTINGS", onClick = onSettings) }
        }
    }
}

@Composable
fun ConnectionLine(connection: ConnectionState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(connection.color(), CircleShape),
        )
        Text(text = connection.label(), color = connection.color(), fontSize = 32.sp)
    }
}

@Composable
fun InfoLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = AlarmColors.Muted, fontSize = 18.sp)
        Text(text = value.ifBlank { "—" }, color = AlarmColors.Text, fontSize = 28.sp)
    }
}

fun ConnectionState.label(): String = when (this) {
    ConnectionState.Offline -> "OFFLINE"
    ConnectionState.Connecting -> "CONNECTING"
    ConnectionState.Connected -> "CONNECTED"
    ConnectionState.Disconnected -> "DISCONNECTED"
    is ConnectionState.Error -> "ERROR"
}

fun ConnectionState.color() = when (this) {
    ConnectionState.Connected -> AlarmColors.Connected
    ConnectionState.Connecting -> AlarmColors.Warning
    else -> AlarmColors.Critical
}
