package com.wayscompany.webhookalarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wayscompany.webhookalarm.model.AlertEvent
import com.wayscompany.webhookalarm.model.Severity
import com.wayscompany.webhookalarm.ui.components.LabeledValue
import com.wayscompany.webhookalarm.ui.components.ScreenFooter
import com.wayscompany.webhookalarm.ui.components.ScreenHeader
import com.wayscompany.webhookalarm.ui.components.ScreenShell
import com.wayscompany.webhookalarm.ui.components.SectionTitle
import com.wayscompany.webhookalarm.ui.components.StatusBadge
import com.wayscompany.webhookalarm.ui.components.TvPanel
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
import com.wayscompany.webhookalarm.ui.theme.AlarmDimens
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
    ScreenShell {
        item {
            ScreenHeader(
                title = "WEBHOOK ALARM",
                trailing = { StatusBadge(connection) },
            )
        }
        item {
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
