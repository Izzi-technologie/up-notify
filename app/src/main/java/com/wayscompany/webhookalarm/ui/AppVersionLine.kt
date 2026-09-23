package com.wayscompany.webhookalarm.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.Text
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
import com.wayscompany.webhookalarm.ui.theme.AlarmTypography
import com.wayscompany.webhookalarm.utils.AppVersion

@Composable
fun AppVersionLine(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val label = remember { AppVersion.label(context) }
    Text(
        text = "App $label",
        modifier = modifier,
        color = AlarmColors.Muted,
        style = AlarmTypography.caption,
    )
}
