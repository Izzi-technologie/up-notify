package com.wayscompany.webhookalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.wayscompany.webhookalarm.alarm.AlertPresentation
import com.wayscompany.webhookalarm.alarm.AlertState
import com.wayscompany.webhookalarm.alarm.presentationFor
import com.wayscompany.webhookalarm.model.AlertEvent
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
import com.wayscompany.webhookalarm.utils.displayTime

@Composable
fun AlertOverlay(
    state: AlertState,
    onAcknowledge: () -> Unit,
) {
    when (state) {
        AlertState.Idle -> Unit
        is AlertState.Active -> when (presentationFor(state.event.severity)) {
            AlertPresentation.BANNER -> InfoBanner(state.event)
            AlertPresentation.OVERLAY -> WarningOverlay(state.event)
            AlertPresentation.FULLSCREEN -> CriticalScreen(
                event = state.event,
                acknowledged = false,
                onAcknowledge = onAcknowledge,
            )
        }
        is AlertState.Acknowledged -> CriticalScreen(
            event = state.event,
            acknowledged = true,
            onAcknowledge = {},
        )
        is AlertState.Resolved -> ResolvedScreen(state.event)
    }
}

@Composable
private fun InfoBanner(event: AlertEvent) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .widthIn(max = 900.dp)
                .fillMaxWidth()
                .background(AlarmColors.InfoBackground, RoundedCornerShape(16.dp))
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = "INFO", color = AlarmColors.Focus, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(text = event.title, color = AlarmColors.Text, fontSize = 28.sp)
            if (event.message.isNotBlank() && event.message != event.title) {
                Text(text = event.message, color = AlarmColors.Muted, fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun WarningOverlay(event: AlertEvent) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlarmColors.Background.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 980.dp)
                .fillMaxWidth(0.8f)
                .background(AlarmColors.WarningBackground, RoundedCornerShape(24.dp))
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "WARNING", color = AlarmColors.Warning, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            Text(text = event.title, color = AlarmColors.Text, fontSize = 40.sp, textAlign = TextAlign.Center)
            if (event.message.isNotBlank()) {
                Text(text = event.message, color = AlarmColors.Text, fontSize = 28.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun CriticalScreen(
    event: AlertEvent,
    acknowledged: Boolean,
    onAcknowledge: () -> Unit,
) {
    val requester = remember(event.id, acknowledged) { FocusRequester() }
    LaunchedEffect(event.id, acknowledged) {
        if (!acknowledged) {
            runCatching { requester.requestFocus() }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlarmColors.CriticalBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 1100.dp)
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = if (acknowledged) "ACKNOWLEDGED" else "CRITICAL",
                color = AlarmColors.Critical,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = event.title.uppercase(),
                color = AlarmColors.Text,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (event.message.isNotBlank()) {
                Text(
                    text = event.message,
                    color = AlarmColors.Text,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center,
                )
            }
            val time = displayTime(event.timestamp)
            if (time != null) {
                Text(text = "Since $time", color = AlarmColors.Muted, fontSize = 24.sp)
            }
            if (!acknowledged) {
                Box(modifier = Modifier.padding(top = 24.dp).widthIn(max = 520.dp).fillMaxWidth()) {
                    TvButton(
                        text = "ACKNOWLEDGE",
                        onClick = onAcknowledge,
                        modifier = Modifier.focusRequester(requester),
                    )
                }
            }
        }
    }
}

@Composable
private fun ResolvedScreen(event: AlertEvent) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlarmColors.Background.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "RESOLVED", color = AlarmColors.Connected, fontSize = 64.sp, fontWeight = FontWeight.Bold)
            Text(text = event.title, color = AlarmColors.Text, fontSize = 36.sp, textAlign = TextAlign.Center)
        }
    }
}
