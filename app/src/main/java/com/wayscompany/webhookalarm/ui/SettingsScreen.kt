package com.wayscompany.webhookalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.wayscompany.webhookalarm.model.Severity
import com.wayscompany.webhookalarm.settings.AppSettings
import com.wayscompany.webhookalarm.settings.SeverityPolicy
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlarmColors.Background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 920.dp),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Text(text = "SETTINGS", color = AlarmColors.Text, fontSize = 48.sp) }
            item { SectionTitle("CONNECTION") }
            item {
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
            }
            item {
                TvTextField(
                    label = "Device ID",
                    value = draft.deviceId,
                    onValueChange = {
                        draft = draft.copy(deviceId = it)
                        saved = false
                    },
                )
            }
            item {
                TvTextField(
                    label = "WebSocket URL",
                    value = draft.webSocketUrl,
                    onValueChange = {
                        draft = draft.copy(webSocketUrl = it)
                        saved = false
                    },
                )
            }
            item {
                TvTextField(
                    label = "Token (optional)",
                    value = draft.authToken,
                    onValueChange = {
                        draft = draft.copy(authToken = it)
                        saved = false
                    },
                )
            }
            item { SectionTitle("ALERTS") }
            item { PolicyEditor(title = "Info", policy = draft.policies.info, showAck = false) { policy ->
                draft = draft.copy(policies = draft.policies.withPolicy(Severity.INFO, policy))
                saved = false
            } }
            item { PolicyEditor(title = "Warning", policy = draft.policies.warning, showAck = false) { policy ->
                draft = draft.copy(policies = draft.policies.withPolicy(Severity.WARNING, policy))
                saved = false
            } }
            item { PolicyEditor(title = "Critical", policy = draft.policies.critical, showAck = true) { policy ->
                draft = draft.copy(policies = draft.policies.withPolicy(Severity.CRITICAL, policy))
                saved = false
            } }
            item { SectionTitle("SYSTEM") }
            item { TvButton(text = "TEST CONNECTION", onClick = { onTestConnection(draft) }) }
            item { TvButton(text = "TEST CRITICAL ALERT", onClick = { onTestCritical(draft) }) }
            item { TvButton(text = "RESTART SERVICE", onClick = { onRestart(draft) }) }
            item {
                TvButton(text = "SAVE", onClick = {
                    onSave(draft)
                    saved = true
                })
            }
            if (saved) {
                item { Text(text = "Saved", color = AlarmColors.Connected, fontSize = 22.sp) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, color = AlarmColors.Focus, fontSize = 22.sp)
}

@Composable
private fun PolicyEditor(
    title: String,
    policy: SeverityPolicy,
    showAck: Boolean,
    onChange: (SeverityPolicy) -> Unit,
) {
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, color = AlarmColors.Text, fontSize = 28.sp)
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
            )
            TvButton(
                text = "Require acknowledge: ${if (policy.requireAck) "ON" else "OFF"}",
                onClick = { onChange(policy.copy(requireAck = !policy.requireAck)) },
            )
        }
    }
}

private fun nextSound(current: String): String {
    val index = soundNames.indexOf(current).let { if (it < 0) 0 else it }
    return soundNames[(index + 1) % soundNames.size]
}
