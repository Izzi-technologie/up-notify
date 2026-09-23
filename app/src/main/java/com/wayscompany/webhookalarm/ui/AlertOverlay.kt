package com.wayscompany.webhookalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
    onDismiss: () -> Unit,
) {
    when (state) {
        AlertState.Idle -> Unit
        is AlertState.Active -> when (presentationFor(state.event.severity)) {
            AlertPresentation.BANNER -> InfoBanner(state.event)
            AlertPresentation.OVERLAY -> WarningCard(state.event, onDismiss)
            AlertPresentation.FULLSCREEN -> CriticalCard(
                event = state.event,
                onAcknowledge = onAcknowledge,
                onDismiss = onDismiss,
            )
        }
        is AlertState.Acknowledged -> AcknowledgedBanner(state.event, onDismiss)
        is AlertState.Resolved -> ResolvedCard(state.event, onDismiss)
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
private fun WarningCard(event: AlertEvent, onDismiss: () -> Unit) {
    Scrim(alpha = 0.4f) {
        AlertCard(background = AlarmColors.WarningBackground) {
            Text(text = "WARNING", color = AlarmColors.Warning, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            Text(text = event.title, color = AlarmColors.Text, fontSize = 36.sp, textAlign = TextAlign.Center)
            if (event.message.isNotBlank()) {
                Text(text = event.message, color = AlarmColors.Text, fontSize = 26.sp, textAlign = TextAlign.Center)
            }
            CloseButton(onDismiss)
        }
    }
}

@Composable
private fun CriticalCard(
    event: AlertEvent,
    onAcknowledge: () -> Unit,
    onDismiss: () -> Unit,
) {
    val requester = remember(event.id) { FocusRequester() }
    LaunchedEffect(event.id) {
        runCatching { requester.requestFocus() }
    }
    Scrim(alpha = 0.4f) {
        AlertCard(background = AlarmColors.CriticalBackground) {
            Text(
                text = "CRITICAL",
                color = AlarmColors.Critical,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = event.title.uppercase(),
                color = AlarmColors.Text,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (event.message.isNotBlank()) {
                Text(
                    text = event.message,
                    color = AlarmColors.Text,
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center,
                )
            }
            val time = displayTime(event.timestamp)
            if (time != null) {
                Text(text = "Since $time", color = AlarmColors.Muted, fontSize = 22.sp)
            }
            TvButton(
                text = "ACKNOWLEDGE",
                onClick = onAcknowledge,
                modifier = Modifier.focusRequester(requester),
            )
            CloseButton(onDismiss, requestFocus = false)
        }
    }
}

@Composable
private fun AcknowledgedBanner(event: AlertEvent, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .widthIn(max = 900.dp)
                .fillMaxWidth()
                .background(AlarmColors.CriticalBackground, RoundedCornerShape(16.dp))
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "ACKNOWLEDGED",
                color = AlarmColors.Critical,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(text = event.title, color = AlarmColors.Text, fontSize = 28.sp)
            if (event.message.isNotBlank() && event.message != event.title) {
                Text(text = event.message, color = AlarmColors.Muted, fontSize = 22.sp)
            }
            CloseButton(onDismiss)
        }
    }
}

@Composable
private fun ResolvedCard(event: AlertEvent, onDismiss: () -> Unit) {
    Scrim(alpha = 0.4f) {
        AlertCard(background = AlarmColors.Surface) {
            Text(text = "RESOLVED", color = AlarmColors.Connected, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            Text(text = event.title, color = AlarmColors.Text, fontSize = 32.sp, textAlign = TextAlign.Center)
            CloseButton(onDismiss)
        }
    }
}

@Composable
private fun Scrim(alpha: Float, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlarmColors.Background.copy(alpha = alpha)),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun AlertCard(background: Color, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 920.dp)
            .fillMaxWidth(0.72f)
            .focusGroup()
            .focusProperties { onExit = { cancelFocusChange() } }
            .background(background, RoundedCornerShape(24.dp))
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
    }
}

@Composable
private fun CloseButton(onDismiss: () -> Unit, requestFocus: Boolean = true, modifier: Modifier = Modifier) {
    val requester = remember { FocusRequester() }
    LaunchedEffect(requestFocus) {
        if (requestFocus) runCatching { requester.requestFocus() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TvButton(
            text = "CLOSE",
            onClick = onDismiss,
            modifier = modifier.focusRequester(requester),
        )
        Text(
            text = "BACK closes",
            color = AlarmColors.Muted,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
