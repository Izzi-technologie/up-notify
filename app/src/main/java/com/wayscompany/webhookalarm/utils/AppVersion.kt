package com.wayscompany.webhookalarm.utils

import android.content.Context
import android.os.Build

object AppVersion {
    fun label(context: Context): String {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        return "v${info.versionName} ($code)"
    }
}
