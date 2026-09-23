package com.wayscompany.webhookalarm.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
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
        typography = Typography(
            displaySmall = AlarmTypography.screenTitle,
            headlineSmall = AlarmTypography.sectionTitle,
            titleMedium = AlarmTypography.status,
            bodyLarge = AlarmTypography.bodyLarge,
            bodyMedium = AlarmTypography.body,
            bodySmall = AlarmTypography.caption,
            labelLarge = AlarmTypography.label,
            labelMedium = AlarmTypography.caption,
            labelSmall = AlarmTypography.caption,
        ),
        content = content,
    )
}
