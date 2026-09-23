package com.wayscompany.webhookalarm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wayscompany.webhookalarm.utils.isTelevisionDevice

@Composable
fun IzziWebhookAlarmTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dimens = remember(context) {
        if (context.isTelevisionDevice()) AlarmDimensValues.Tv else AlarmDimensValues.Phone
    }
    CompositionLocalProvider(LocalAlarmDimens provides dimens) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = AlarmColors.Text,
                onPrimary = AlarmColors.Background,
                background = AlarmColors.Background,
                onBackground = AlarmColors.Text,
                surface = AlarmColors.Surface,
                onSurface = AlarmColors.Text,
                outline = AlarmColors.Focus,
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
}
