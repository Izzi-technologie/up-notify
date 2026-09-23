package com.wayscompany.webhookalarm.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wayscompany.webhookalarm.model.AlertEvent
import com.wayscompany.webhookalarm.model.Severity
import com.wayscompany.webhookalarm.ui.components.CopyableUrlRow
import com.wayscompany.webhookalarm.ui.components.PhoneAlertPermissionsCard
import com.wayscompany.webhookalarm.ui.components.LabeledValue
import com.wayscompany.webhookalarm.ui.components.ScreenFooter
import com.wayscompany.webhookalarm.ui.components.ScreenHeader
import com.wayscompany.webhookalarm.ui.components.ScreenShell
import com.wayscompany.webhookalarm.ui.components.SectionTitle
import com.wayscompany.webhookalarm.ui.components.StatusBadge
import com.wayscompany.webhookalarm.ui.components.TvPanel
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
import com.wayscompany.webhookalarm.ui.theme.AlarmDimens
import com.wayscompany.webhookalarm.utils.deriveWebhookUrl
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
    onRequestNotifications: () -> Unit,
) {
    ScreenShell {
        item {
            PhoneAlertPermissionsCard(onRequestNotifications = onRequestNotifications)
        }
        item {
            HomeOverview(
                connection = connection,
                deviceId = deviceId,
                serverUrl = serverUrl,
                lastEvent = lastEvent,
            )
        }
        item {
            WebhookSection(serverUrl = serverUrl, deviceId = deviceId)
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AlarmDimens.itemGap),
            ) {
                SectionTitle(text = "TESTS")
                TvButton(
                    text = "TEST INFO",
                    onClick = { onTest(Severity.INFO) },
                    tint = AlarmColors.Focus,
                )
                TvButton(
                    text = "TEST WARNING",
                    onClick = { onTest(Severity.WARNING) },
                    tint = AlarmColors.Warning,
                )
                TvButton(
                    text = "TEST CRITICAL",
                    onClick = { onTest(Severity.CRITICAL) },
                    tint = AlarmColors.Critical,
                )
            }
        }
        item {
            TvButton(
                text = "SETTINGS",
                onClick = onSettings,
                variant = TvButtonVariant.Secondary,
            )
        }
        item { ScreenFooter() }
    }
}

@Composable
private fun HomeOverview(
    connection: ConnectionState,
    deviceId: String,
    serverUrl: String,
    lastEvent: AlertEvent?,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .border(
                width = 2.dp,
                color = if (focused) AlarmColors.Focus else Color.Transparent,
                shape = RoundedCornerShape(AlarmDimens.cornerRadius),
            ),
        verticalArrangement = Arrangement.spacedBy(AlarmDimens.sectionGap),
    ) {
        ScreenHeader(
            title = "IZZI WEBHOOK ALARM",
            trailing = { StatusBadge(connection) },
        )
        TvPanel {
            LabeledValue(label = "Device", value = deviceId)
            LabeledValue(label = "Server", value = serverUrl)
            LabeledValue(
                label = "Last event",
                value = lastEvent?.message?.ifBlank { lastEvent.title } ?: "None",
                caption = displayTime(lastEvent?.timestamp),
            )
        }
    }
}

@Composable
private fun WebhookSection(serverUrl: String, deviceId: String) {
    val webhookUrl = deriveWebhookUrl(serverUrl, deviceId)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AlarmDimens.itemGap),
    ) {
        SectionTitle(text = "WEBHOOK")
        TvPanel {
            CopyableUrlRow(
                label = "URL for monitoring services",
                url = webhookUrl,
                caption = "POST JSON alerts to this URL (e.g. Checkmate).",
            )
        }
    }
}
