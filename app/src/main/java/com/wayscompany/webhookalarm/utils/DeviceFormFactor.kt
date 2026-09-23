package com.wayscompany.webhookalarm.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

fun Context.isTelevisionDevice(): Boolean {
    val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    if (uiMode == Configuration.UI_MODE_TYPE_TELEVISION) return true
    return packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}
