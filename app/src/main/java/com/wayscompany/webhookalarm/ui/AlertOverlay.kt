package com.wayscompany.webhookalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Text
import com.wayscompany.webhookalarm.alarm.AlertPresentation
import com.wayscompany.webhookalarm.alarm.AlertState
import com.wayscompany.webhookalarm.alarm.presentationFor
import com.wayscompany.webhookalarm.model.AlertEvent
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
import com.wayscompany.webhookalarm.ui.theme.AlarmDimens
import com.wayscompany.webhookalarm.ui.theme.AlarmTypography
import com.wayscompany.webhookalarm.utils.displayTime

private const val ScrimAlpha = 0.78f

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
    TopBanner(
        background = AlarmColors.InfoBackground,
        borderColor = AlarmColors.Focus,
    ) {
        Text(
            text = "INFO",
            color = AlarmColors.Focus,
            style = AlarmTypography.sectionTitle,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = event.title,
            color = AlarmColors.Text,
            style = AlarmTypography.bodyLarge,
        )
        if (event.message.isNotBlank() && event.message != event.title) {
            Text(
                text = event.message,
                color = AlarmColors.Muted,
                style = AlarmTypography.body,
            )
        }
    }
}

@Composable
private fun WarningCard(event: AlertEvent, onDismiss: () -> Unit) {
    Scrim {
        AlertCard(
            background = AlarmColors.WarningBackground,
            borderColor = AlarmColors.Warning,
        ) {
            Text(
                text = "WARNING",
                color = AlarmColors.Warning,
                style = AlarmTypography.screenTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = event.title,
                color = AlarmColors.Text,
                style = AlarmTypography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (event.message.isNotBlank()) {
                Text(
                    text = event.message,
                    color = AlarmColors.Text,
                    style = AlarmTypography.body,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
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
    Scrim {
        AlertCard(
            background = AlarmColors.CriticalBackground,
            borderColor = AlarmColors.Critical,
        ) {
            Text(
                text = "CRITICAL",
                color = AlarmColors.Critical,
                style = AlarmTypography.screenTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = event.title.uppercase(),
                color = AlarmColors.Text,
                style = AlarmTypography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (event.message.isNotBlank()) {
                Text(
                    text = event.message,
                    color = AlarmColors.Text,
                    style = AlarmTypography.body,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val time = displayTime(event.timestamp)
            if (time != null) {
                Text(
                    text = "Since $time",
                    color = AlarmColors.Muted,
                    style = AlarmTypography.caption,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TvButton(
                text = "ACKNOWLEDGE",
                onClick = onAcknowledge,
                modifier = Modifier.focusRequester(requester),
                variant = TvButtonVariant.Primary,
            )
            CloseButton(onDismiss, requestFocus = false)
        }
    }
}

@Composable
private fun AcknowledgedBanner(event: AlertEvent, onDismiss: () -> Unit) {
    TopBanner(
        background = AlarmColors.CriticalBackground,
        borderColor = AlarmColors.Critical,
    ) {
        Text(
            text = "ACKNOWLEDGED",
            color = AlarmColors.Critical,
            style = AlarmTypography.sectionTitle,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = event.title,
            color = AlarmColors.Text,
            style = AlarmTypography.bodyLarge,
        )
        if (event.message.isNotBlank() && event.message != event.title) {
            Text(
                text = event.message,
                color = AlarmColors.Muted,
                style = AlarmTypography.body,
            )
        }
        CloseButton(onDismiss)
    }
}

@Composable
private fun ResolvedCard(event: AlertEvent, onDismiss: () -> Unit) {
    Scrim {
        AlertCard(
            background = AlarmColors.Surface,
            borderColor = AlarmColors.Connected,
        ) {
            Text(
                text = "RESOLVED",
                color = AlarmColors.Connected,
                style = AlarmTypography.screenTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = event.title,
                color = AlarmColors.Text,
                style = AlarmTypography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            CloseButton(onDismiss)
        }
    }
}

@Composable
private fun Scrim(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlarmColors.Background.copy(alpha = ScrimAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun TopBanner(
    background: Color,
    borderColor: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(AlarmDimens.cornerRadius)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .padding(top = AlarmDimens.screenPaddingV)
                .widthIn(max = AlarmDimens.contentMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = AlarmDimens.screenPaddingH)
                .border(width = 1.dp, color = borderColor, shape = shape)
                .background(background, shape)
                .padding(AlarmDimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AlarmDimens.itemGap),
            content = content,
        )
    }
}

@Composable
private fun AlertCard(
    background: Color,
    borderColor: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(AlarmDimens.cornerRadius)
    Column(
        modifier = Modifier
            .widthIn(max = AlarmDimens.contentMaxWidth)
            .fillMaxWidth(0.72f)
            .focusGroup()
            .focusProperties { onExit = { cancelFocusChange() } }
            .border(width = 1.dp, color = borderColor, shape = shape)
            .background(background, shape)
            .padding(AlarmDimens.cardPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AlarmDimens.itemGap),
        content = content,
    )
}

@Composable
private fun CloseButton(
    onDismiss: () -> Unit,
    requestFocus: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val requester = remember { FocusRequester() }
    LaunchedEffect(requestFocus) {
        if (requestFocus) runCatching { requester.requestFocus() }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TvButton(
            text = "CLOSE",
            onClick = onDismiss,
            modifier = modifier.focusRequester(requester),
            variant = TvButtonVariant.Secondary,
        )
        Text(
            text = "BACK closes",
            modifier = Modifier.fillMaxWidth(),
            color = AlarmColors.Muted,
            style = AlarmTypography.caption,
            textAlign = TextAlign.Center,
        )
    }
}
