package com.wayscompany.webhookalarm.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
import com.wayscompany.webhookalarm.ui.theme.AlarmDimens
import com.wayscompany.webhookalarm.ui.theme.AlarmTypography

enum class TvButtonVariant {
    Primary,
    Secondary,
    Danger,
}

@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: TvButtonVariant = TvButtonVariant.Primary,
    tint: Color? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(AlarmDimens.buttonCornerRadius)
    val palette = buttonPalette(variant, tint)
    val container = if (focused) palette.focusedContainer else palette.container
    val content = if (focused) palette.focusedContent else palette.content
    val borderColor = if (focused) palette.focusedBorder else palette.border
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(AlarmDimens.buttonMinHeight)
            .onFocusChanged { focused = it.isFocused }
            .border(width = if (focused) 2.dp else 1.dp, color = borderColor, shape = shape),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = AlarmTypography.body.copy(
                    lineHeight = AlarmTypography.body.fontSize,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun TvTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(AlarmDimens.cornerRadius)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            color = AlarmColors.Muted,
            style = AlarmTypography.caption,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = AlarmTypography.bodyLarge.merge(TextStyle(color = AlarmColors.Text)),
            cursorBrush = SolidColor(AlarmColors.Focus),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AlarmDimens.buttonMinHeight)
                .onFocusChanged { focused = it.isFocused }
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color = if (focused) AlarmColors.Focus else AlarmColors.Border,
                    shape = shape,
                )
                .background(AlarmColors.SurfaceElevated, shape)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}

@Composable
fun TvVolumeBar(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(AlarmDimens.cornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onValueChange((value - 5).coerceIn(0, 100))
                        true
                    }
                    Key.DirectionRight -> {
                        onValueChange((value + 5).coerceIn(0, 100))
                        true
                    }
                    else -> false
                }
            }
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) AlarmColors.Focus else AlarmColors.Border,
                shape = shape,
            )
            .background(AlarmColors.SurfaceElevated, shape)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = AlarmColors.Text,
                style = AlarmTypography.body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$value%",
                color = AlarmColors.Text,
                style = AlarmTypography.body,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(AlarmColors.Border, RoundedCornerShape(9.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value.coerceIn(0, 100) / 100f)
                    .background(AlarmColors.Focus, RoundedCornerShape(9.dp)),
            )
        }
    }
}

@Composable
fun TvChoice(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            color = AlarmColors.Muted,
            style = AlarmTypography.caption,
        )
        TvButton(
            text = value,
            onClick = onClick,
            variant = TvButtonVariant.Secondary,
        )
    }
}

@Composable
fun InlineAdjust(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AlarmDimens.itemGap),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            TvButton(
                text = "−",
                onClick = onPrevious,
                variant = TvButtonVariant.Secondary,
            )
        }
        Text(
            text = label,
            modifier = Modifier.widthIn(min = AlarmDimens.inlineLabelMinWidth),
            color = AlarmColors.Text,
            style = AlarmTypography.body,
            textAlign = TextAlign.Center,
        )
        Box(modifier = Modifier.weight(1f)) {
            TvButton(
                text = "+",
                onClick = onNext,
                variant = TvButtonVariant.Secondary,
            )
        }
    }
}

private data class ButtonPalette(
    val container: Color,
    val content: Color,
    val focusedContainer: Color,
    val focusedContent: Color,
    val border: Color,
    val focusedBorder: Color,
)

private fun buttonPalette(variant: TvButtonVariant, tint: Color?): ButtonPalette {
    if (tint != null) {
        return ButtonPalette(
            container = tintedSurface(tint),
            content = AlarmColors.Text,
            focusedContainer = tint,
            focusedContent = tint.preferredContent(),
            border = tint,
            focusedBorder = AlarmColors.Text,
        )
    }
    return when (variant) {
        TvButtonVariant.Primary -> ButtonPalette(
            container = AlarmColors.Focus,
            content = AlarmColors.Background,
            focusedContainer = AlarmColors.Text,
            focusedContent = AlarmColors.Background,
            border = AlarmColors.Focus,
            focusedBorder = AlarmColors.Text,
        )
        TvButtonVariant.Secondary -> ButtonPalette(
            container = Color.Transparent,
            content = AlarmColors.Text,
            focusedContainer = AlarmColors.SurfaceElevated,
            focusedContent = AlarmColors.Text,
            border = AlarmColors.Border,
            focusedBorder = AlarmColors.Focus,
        )
        TvButtonVariant.Danger -> ButtonPalette(
            container = AlarmColors.CriticalBackground,
            content = AlarmColors.Critical,
            focusedContainer = AlarmColors.Critical,
            focusedContent = AlarmColors.Text,
            border = AlarmColors.Critical,
            focusedBorder = AlarmColors.Text,
        )
    }
}

private fun tintedSurface(tint: Color): Color = when (tint) {
    AlarmColors.Focus -> AlarmColors.InfoBackground
    AlarmColors.Warning -> AlarmColors.WarningBackground
    AlarmColors.Critical -> AlarmColors.CriticalBackground
    AlarmColors.Connected -> AlarmColors.AccentSubtle
    else -> tint.copy(alpha = 0.18f)
}

private fun Color.preferredContent(): Color {
    val luminance = (0.2126f * red) + (0.7152f * green) + (0.0722f * blue)
    return if (luminance >= 0.62f) AlarmColors.Background else AlarmColors.Text
}
