package com.wayscompany.webhookalarm.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.wayscompany.webhookalarm.R
import com.wayscompany.webhookalarm.ui.TvButton
import com.wayscompany.webhookalarm.ui.TvButtonVariant
import com.wayscompany.webhookalarm.ui.theme.AlarmColors
import com.wayscompany.webhookalarm.ui.theme.AlarmDimens
import com.wayscompany.webhookalarm.ui.theme.AlarmTypography
import com.wayscompany.webhookalarm.utils.PhoneAlertReadiness
import com.wayscompany.webhookalarm.utils.isTelevisionDevice
import com.wayscompany.webhookalarm.utils.openBatteryOptimizationSettings
import com.wayscompany.webhookalarm.utils.openFullScreenIntentSettings
import com.wayscompany.webhookalarm.utils.openNotificationSettings
import com.wayscompany.webhookalarm.utils.openOverlayPermissionSettings
import com.wayscompany.webhookalarm.utils.phoneAlertReadiness

@Composable
fun rememberPhoneAlertReadiness(): PhoneAlertReadiness {
    val context = LocalContext.current
    var generation by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(context) {
        generation++
        onPauseOrDispose { }
    }
    return remember(context, generation) { context.phoneAlertReadiness() }
}

@Composable
fun PhoneAlertPermissionsCard(
    readiness: PhoneAlertReadiness,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    if (context.isTelevisionDevice() || readiness.allGranted) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AlarmDimens.itemGap),
    ) {
        SectionTitle(text = stringResource(R.string.phone_permissions_title))
        TvPanel {
            Text(
                text = stringResource(R.string.phone_permissions_body),
                color = AlarmColors.Muted,
                style = AlarmTypography.body,
            )
            PermissionRow(
                label = stringResource(R.string.phone_permissions_notifications),
                granted = readiness.notificationsGranted,
                onGrant = { context.openNotificationSettings() },
            )
            PermissionRow(
                label = stringResource(R.string.phone_permissions_overlay),
                granted = readiness.overlayGranted,
                onGrant = { context.openOverlayPermissionSettings() },
            )
            PermissionRow(
                label = stringResource(R.string.phone_permissions_full_screen),
                granted = readiness.fullScreenIntentGranted,
                onGrant = { context.openFullScreenIntentSettings() },
            )
            PermissionRow(
                label = stringResource(R.string.phone_permissions_battery),
                granted = readiness.batteryExempt,
                onGrant = { context.openBatteryOptimizationSettings() },
            )
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    onGrant: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$label: ${if (granted) "OK" else "Required"}",
            color = if (granted) AlarmColors.Connected else AlarmColors.Warning,
            style = AlarmTypography.bodyLarge,
        )
        if (!granted) {
            TvButton(
                text = stringResource(R.string.phone_permissions_grant),
                onClick = onGrant,
                variant = TvButtonVariant.Secondary,
            )
        }
    }
}
