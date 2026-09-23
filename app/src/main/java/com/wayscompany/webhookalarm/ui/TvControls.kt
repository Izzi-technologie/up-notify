package com.wayscompany.webhookalarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.wayscompany.webhookalarm.ui.theme.AlarmColors

@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
    ) {
        Text(text = text, fontSize = 22.sp)
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
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        Text(text = label, color = AlarmColors.Muted, fontSize = 18.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = AlarmColors.Text, fontSize = 28.sp),
            cursorBrush = SolidColor(AlarmColors.Focus),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color = if (focused) AlarmColors.Focus else AlarmColors.Border,
                    shape = RoundedCornerShape(12.dp),
                )
                .background(AlarmColors.Surface, RoundedCornerShape(12.dp))
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
                shape = RoundedCornerShape(12.dp),
            )
            .background(AlarmColors.Surface, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
    ) {
        Text(text = "$label  $value%", color = AlarmColors.Text, fontSize = 22.sp)
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
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        Text(text = label, color = AlarmColors.Muted, fontSize = 18.sp)
        TvButton(text = value, onClick = onClick)
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
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.weight(1f)) {
            TvButton(text = "−", onClick = onPrevious)
        }
        Text(text = label, color = AlarmColors.Text, fontSize = 22.sp)
        Box(Modifier.weight(1f)) {
            TvButton(text = "+", onClick = onNext)
        }
    }
}
