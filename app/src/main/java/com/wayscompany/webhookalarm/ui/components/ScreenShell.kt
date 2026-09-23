package com.wayscompany.webhookalarm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.compose.foundation.BorderStroke
import com.wayscompany.webhookalarm.ui.AppVersionLine
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
import com.wayscompany.webhookalarm.ui.theme.AlarmDimens
import com.wayscompany.webhookalarm.ui.theme.AlarmTypography
import com.wayscompany.webhookalarm.websocket.ConnectionState

@Composable
fun ScreenShell(
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AlarmColors.Background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = AlarmDimens.contentMaxWidth)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = AlarmDimens.screenPaddingH,
                vertical = AlarmDimens.screenPaddingV,
            ),
            verticalArrangement = Arrangement.spacedBy(AlarmDimens.sectionGap),
            content = content,
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    supporting: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AlarmDimens.itemGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = AlarmColors.Text,
                style = AlarmTypography.screenTitle,
            )
            if (trailing != null) {
                Box(modifier = Modifier.padding(start = AlarmDimens.itemGap)) {
                    trailing()
                }
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = AlarmColors.Muted,
                style = AlarmTypography.bodyLarge,
            )
        }
        if (supporting != null) {
            supporting()
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = AlarmColors.Focus,
        style = AlarmTypography.sectionTitle,
    )
}

@Composable
fun TvPanel(
    modifier: Modifier = Modifier,
    containerColor: Color = AlarmColors.Surface,
    borderColor: Color = AlarmColors.Border,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(AlarmDimens.cornerRadius)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = SurfaceDefaults.colors(
            containerColor = containerColor,
            contentColor = AlarmColors.Text,
        ),
        border = Border(
            border = BorderStroke(width = 1.dp, color = borderColor),
            shape = shape,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AlarmDimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(AlarmDimens.itemGap),
            content = content,
        )
    }
}

@Composable
fun LabeledValue(
    label: String,
    value: String,
    caption: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            color = AlarmColors.Muted,
            style = AlarmTypography.caption,
        )
        Text(
            text = value.ifBlank { "—" },
            color = AlarmColors.Text,
            style = AlarmTypography.bodyLarge,
        )
        if (!caption.isNullOrBlank()) {
            Text(
                text = caption,
                color = AlarmColors.Muted,
                style = AlarmTypography.caption,
            )
        }
    }
}

@Composable
fun StatusBadge(
    connection: ConnectionState,
    modifier: Modifier = Modifier,
) {
    val color = connection.color()
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .background(
                color = if (connection is ConnectionState.Connected) {
                    AlarmColors.AccentSubtle
                } else {
                    AlarmColors.Surface
                },
                shape = shape,
            )
            .border(width = 1.dp, color = color.copy(alpha = 0.55f), shape = shape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, CircleShape),
        )
        Text(
            text = connection.label(),
            color = color,
            style = AlarmTypography.status,
        )
    }
}

@Composable
fun ScreenFooter(modifier: Modifier = Modifier) {
    AppVersionLine(modifier = modifier.fillMaxWidth())
}

private fun ConnectionState.label(): String = when (this) {
    ConnectionState.Offline -> "OFFLINE"
    ConnectionState.Connecting -> "CONNECTING"
    ConnectionState.Connected -> "CONNECTED"
    ConnectionState.Disconnected -> "DISCONNECTED"
    is ConnectionState.Error -> "ERROR"
}

private fun ConnectionState.color(): Color = when (this) {
    ConnectionState.Connected -> AlarmColors.Connected
    ConnectionState.Connecting -> AlarmColors.Warning
    else -> AlarmColors.Critical
}
