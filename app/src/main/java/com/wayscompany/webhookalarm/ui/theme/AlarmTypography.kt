package com.wayscompany.webhookalarm.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class AlarmTypographyValues(
    val screenTitle: TextStyle,
    val sectionTitle: TextStyle,
    val label: TextStyle,
    val body: TextStyle,
    val bodyLarge: TextStyle,
    val caption: TextStyle,
    val status: TextStyle,
) {
    companion object {
        val Tv = AlarmTypographyValues(
            screenTitle = TextStyle(
                fontSize = 48.sp,
                lineHeight = 56.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
            ),
            sectionTitle = TextStyle(
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.1.sp,
            ),
            label = TextStyle(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
            ),
            body = TextStyle(
                fontSize = 22.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Normal,
            ),
            bodyLarge = TextStyle(
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Normal,
            ),
            caption = TextStyle(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
            ),
            status = TextStyle(
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
            ),
        )

        val Phone = AlarmTypographyValues(
            screenTitle = TextStyle(
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
            ),
            sectionTitle = TextStyle(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
            ),
            label = TextStyle(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
            ),
            body = TextStyle(
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
            ),
            bodyLarge = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
            ),
            caption = TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
            ),
            status = TextStyle(
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
            ),
        )
    }
}

val LocalAlarmTypography = staticCompositionLocalOf { AlarmTypographyValues.Tv }

object AlarmTypography {
    private val values: AlarmTypographyValues
        @Composable
        @ReadOnlyComposable
        get() = LocalAlarmTypography.current

    val screenTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = values.screenTitle

    val sectionTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = values.sectionTitle

    val label: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = values.label

    val body: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = values.body

    val bodyLarge: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = values.bodyLarge

    val caption: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = values.caption

    val status: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = values.status
}
