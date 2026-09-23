package com.wayscompany.webhookalarm.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.wayscompany.webhookalarm.ui.TvButton
import com.wayscompany.webhookalarm.ui.TvButtonVariant
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
import com.wayscompany.webhookalarm.ui.theme.AlarmDimens
import com.wayscompany.webhookalarm.ui.theme.AlarmTypography
import kotlinx.coroutines.delay

@Composable
fun CopyableUrlRow(
    label: String,
    url: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AlarmDimens.itemGap),
    ) {
        Text(
            text = label,
            color = AlarmColors.Muted,
            style = AlarmTypography.caption,
        )
        Text(
            text = url.ifBlank { "—" },
            color = AlarmColors.Text,
            style = AlarmTypography.body,
        )
        if (!caption.isNullOrBlank()) {
            Text(
                text = caption,
                color = AlarmColors.Muted,
                style = AlarmTypography.caption,
            )
        }
        TvButton(
            text = if (copied) "COPIED" else "COPY URL",
            onClick = {
                if (url.isBlank()) return@TvButton
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(label, url))
                copied = true
            },
            variant = TvButtonVariant.Secondary,
        )
    }
}
