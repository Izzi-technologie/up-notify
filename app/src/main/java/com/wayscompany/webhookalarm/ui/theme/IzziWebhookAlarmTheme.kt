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
    val isTv = remember(context) { context.isTelevisionDevice() }
    val dimens = remember(isTv) {
        if (isTv) AlarmDimensValues.Tv else AlarmDimensValues.Phone
    }
    val typography = remember(isTv) {
        if (isTv) AlarmTypographyValues.Tv else AlarmTypographyValues.Phone
    }
    CompositionLocalProvider(
        LocalAlarmDimens provides dimens,
        LocalAlarmTypography provides typography,
    ) {
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
                displaySmall = typography.screenTitle,
                headlineSmall = typography.sectionTitle,
                titleMedium = typography.status,
                bodyLarge = typography.bodyLarge,
                bodyMedium = typography.body,
                bodySmall = typography.caption,
                labelLarge = typography.label,
                labelMedium = typography.caption,
                labelSmall = typography.caption,
            ),
            content = content,
        )
    }
}
