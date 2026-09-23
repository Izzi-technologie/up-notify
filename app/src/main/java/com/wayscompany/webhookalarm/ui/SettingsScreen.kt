package com.wayscompany.webhookalarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import com.wayscompany.webhookalarm.model.Severity
import com.wayscompany.webhookalarm.settings.AppSettings
import com.wayscompany.webhookalarm.settings.SeverityPolicy
import com.wayscompany.webhookalarm.ui.components.LabeledValue
import com.wayscompany.webhookalarm.ui.components.ScreenFooter
import com.wayscompany.webhookalarm.ui.components.ScreenHeader
import com.wayscompany.webhookalarm.ui.components.ScreenShell
import com.wayscompany.webhookalarm.ui.components.SectionTitle
import com.wayscompany.webhookalarm.ui.components.TvPanel
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
import com.wayscompany.webhookalarm.ui.theme.AlarmDimens
import com.wayscompany.webhookalarm.ui.theme.AlarmTypography
import com.wayscompany.webhookalarm.utils.deriveWebSocketUrl

private val soundNames = listOf("info", "warning", "critical")

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onTestConnection: (AppSettings) -> Unit,
    onTestCritical: (AppSettings) -> Unit,
    onRestart: (AppSettings) -> Unit,
) {
    var draft by remember(settings) { mutableStateOf(settings) }
    var saved by remember { mutableStateOf(false) }
    ScreenShell {
        item {
            ScreenHeader(
                title = "SETTINGS",
                supporting = { ScreenFooter() },
            )
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AlarmDimens.itemGap),
            ) {
                SectionTitle(text = "CONNECTION")
                TvPanel {
                    TvTextField(
                        label = "Server URL",
                        value = draft.serverUrl,
                        onValueChange = { value ->
                            val derivedBefore = deriveWebSocketUrl(draft.serverUrl)
                            val webSocketUrl = if (draft.webSocketUrl == derivedBefore) {
                                deriveWebSocketUrl(value)
                            } else {
                                draft.webSocketUrl
                            }
                            draft = draft.copy(serverUrl = value, webSocketUrl = webSocketUrl)
                            saved = false
                        },
                    )
                    LabeledValue(
                        label = "Device ID",
                        value = draft.deviceId,
                        caption = "Assigned to this device",
                    )
                    TvTextField(
                        label = "WebSocket URL",
                        value = draft.webSocketUrl,
                        onValueChange = {
                            draft = draft.copy(webSocketUrl = it)
                            saved = false
                        },
                    )
                    TvTextField(
                        label = "Token (optional)",
                        value = draft.authToken,
                        onValueChange = {
                            draft = draft.copy(authToken = it)
                            saved = false
                        },
                    )
                }
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AlarmDimens.itemGap),
            ) {
                SectionTitle(text = "ALERTS")
                PolicyEditor(
                    title = "Info",
                    policy = draft.policies.info,
                    showAck = false,
                ) { policy ->
                    draft = draft.copy(policies = draft.policies.withPolicy(Severity.INFO, policy))
                    saved = false
                }
                PolicyEditor(
                    title = "Warning",
                    policy = draft.policies.warning,
                    showAck = false,
                ) { policy ->
                    draft = draft.copy(policies = draft.policies.withPolicy(Severity.WARNING, policy))
                    saved = false
                }
                PolicyEditor(
                    title = "Critical",
                    policy = draft.policies.critical,
                    showAck = true,
                ) { policy ->
                    draft = draft.copy(policies = draft.policies.withPolicy(Severity.CRITICAL, policy))
                    saved = false
                }
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AlarmDimens.itemGap),
            ) {
                SectionTitle(text = "SYSTEM")
                TvButton(
                    text = "TEST CONNECTION",
                    onClick = { onTestConnection(draft) },
                    variant = TvButtonVariant.Secondary,
                )
                TvButton(
                    text = "TEST CRITICAL ALERT",
                    onClick = { onTestCritical(draft) },
                    variant = TvButtonVariant.Secondary,
                )
                TvButton(
                    text = "RESTART SERVICE",
                    onClick = { onRestart(draft) },
                    variant = TvButtonVariant.Secondary,
                )
                TvButton(
                    text = "SAVE",
                    onClick = {
                        onSave(draft)
                        saved = true
                    },
                    variant = TvButtonVariant.Primary,
                )
                if (saved) {
                    Text(
                        text = "Saved",
                        color = AlarmColors.Connected,
                        style = AlarmTypography.body,
                    )
                }
            }
        }
    }
}

@Composable
private fun PolicyEditor(
    title: String,
    policy: SeverityPolicy,
    showAck: Boolean,
    onChange: (SeverityPolicy) -> Unit,
) {
    TvPanel {
        Text(
            text = title,
            color = AlarmColors.Text,
            style = AlarmTypography.bodyLarge,
        )
        TvChoice(
            label = "Sound",
            value = policy.soundName,
            onClick = { onChange(policy.copy(soundName = nextSound(policy.soundName))) },
        )
        TvVolumeBar(
            label = "$title volume",
            value = policy.volumePercent,
            onValueChange = { onChange(policy.copy(volumePercent = it)) },
        )
        if (!showAck) {
            InlineAdjust(
                label = "Repeat ${policy.repeatCount}",
                onPrevious = { onChange(policy.copy(repeatCount = (policy.repeatCount - 1).coerceAtLeast(1))) },
                onNext = { onChange(policy.copy(repeatCount = (policy.repeatCount + 1).coerceAtMost(10))) },
            )
        }
        if (showAck) {
            TvButton(
                text = "Persistent: ${if (policy.persistent) "ON" else "OFF"}",
                onClick = { onChange(policy.copy(persistent = !policy.persistent)) },
                variant = TvButtonVariant.Secondary,
            )
            TvButton(
                text = "Require acknowledge: ${if (policy.requireAck) "ON" else "OFF"}",
                onClick = { onChange(policy.copy(requireAck = !policy.requireAck)) },
                variant = TvButtonVariant.Secondary,
            )
        }
    }
}

private fun nextSound(current: String): String {
    val index = soundNames.indexOf(current).let { if (it < 0) 0 else it }
    return soundNames[(index + 1) % soundNames.size]
}
