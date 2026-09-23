package com.wayscompany.webhookalarm.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AlarmDimensValues(
    val contentMaxWidth: Dp,
    val screenPaddingH: Dp,
    val screenPaddingV: Dp,
    val cardPadding: Dp,
    val sectionGap: Dp,
    val itemGap: Dp,
    val cornerRadius: Dp,
    val buttonCornerRadius: Dp,
    val buttonMinHeight: Dp,
    val inlineLabelMinWidth: Dp,
) {
    companion object {
        val Tv = AlarmDimensValues(
            contentMaxWidth = 960.dp,
            screenPaddingH = 56.dp,
            screenPaddingV = 40.dp,
            cardPadding = 24.dp,
            sectionGap = 28.dp,
            itemGap = 16.dp,
            cornerRadius = 16.dp,
            buttonCornerRadius = 28.dp,
            buttonMinHeight = 64.dp,
            inlineLabelMinWidth = 220.dp,
        )

        val Phone = AlarmDimensValues(
            contentMaxWidth = 560.dp,
            screenPaddingH = 20.dp,
            screenPaddingV = 24.dp,
            cardPadding = 16.dp,
            sectionGap = 20.dp,
            itemGap = 12.dp,
            cornerRadius = 12.dp,
            buttonCornerRadius = 12.dp,
            buttonMinHeight = 48.dp,
            inlineLabelMinWidth = 120.dp,
        )
    }
}

val LocalAlarmDimens = staticCompositionLocalOf { AlarmDimensValues.Tv }

object AlarmDimens {
    private val values: AlarmDimensValues
        @Composable
        @ReadOnlyComposable
        get() = LocalAlarmDimens.current

    val contentMaxWidth: Dp
        @Composable
        @ReadOnlyComposable
        get() = values.contentMaxWidth

    val screenPaddingH: Dp
        @Composable
        @ReadOnlyComposable
        get() = values.screenPaddingH

    val screenPaddingV: Dp
        @Composable
        @ReadOnlyComposable
        get() = values.screenPaddingV

    val cardPadding: Dp
        @Composable
        @ReadOnlyComposable
        get() = values.cardPadding

    val sectionGap: Dp
        @Composable
        @ReadOnlyComposable
        get() = values.sectionGap

    val itemGap: Dp
        @Composable
        @ReadOnlyComposable
        get() = values.itemGap

    val cornerRadius: Dp
        @Composable
        @ReadOnlyComposable
        get() = values.cornerRadius

    val buttonCornerRadius: Dp
        @Composable
        @ReadOnlyComposable
        get() = values.buttonCornerRadius

    val buttonMinHeight: Dp
        @Composable
        @ReadOnlyComposable
        get() = values.buttonMinHeight

    val inlineLabelMinWidth: Dp
        @Composable
        @ReadOnlyComposable
        get() = values.inlineLabelMinWidth
}
