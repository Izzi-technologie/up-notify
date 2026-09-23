package com.wayscompany.webhookalarm.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection

/**
 * Combines [WindowInsets.safeDrawing] (cutout, system bars, TV overscan) with app screen padding.
 */
object AlarmSafeInsets {
    @Composable
    fun screenContentPadding(): PaddingValues {
        val layoutDirection = LocalLayoutDirection.current
        val safe = WindowInsets.safeDrawing.asPaddingValues()
        return PaddingValues(
            start = AlarmDimens.screenPaddingH + safe.calculateStartPadding(layoutDirection),
            top = AlarmDimens.screenPaddingV + safe.calculateTopPadding(),
            end = AlarmDimens.screenPaddingH + safe.calculateEndPadding(layoutDirection),
            bottom = AlarmDimens.screenPaddingV + safe.calculateBottomPadding(),
        )
    }

    @Composable
    fun overlayContentPadding(): PaddingValues = screenContentPadding()

    @Composable
    fun topBannerPadding(): PaddingValues {
        val layoutDirection = LocalLayoutDirection.current
        val safe = WindowInsets.safeDrawing.asPaddingValues()
        return PaddingValues(
            start = AlarmDimens.screenPaddingH + safe.calculateStartPadding(layoutDirection),
            top = AlarmDimens.screenPaddingV + safe.calculateTopPadding(),
            end = AlarmDimens.screenPaddingH + safe.calculateEndPadding(layoutDirection),
        )
    }
}
