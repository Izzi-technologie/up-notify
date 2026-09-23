package com.wayscompany.webhookalarm.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@Composable
fun WebhookAlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AlarmColors.Text,
            onPrimary = AlarmColors.Background,
            background = AlarmColors.Background,
            onBackground = AlarmColors.Text,
            surface = AlarmColors.Surface,
            onSurface = AlarmColors.Text,
            border = AlarmColors.Focus,
        ),
        content = content,
    )
}
