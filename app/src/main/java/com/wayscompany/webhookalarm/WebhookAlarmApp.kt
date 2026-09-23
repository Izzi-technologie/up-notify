package com.wayscompany.webhookalarm

import android.app.Application
import com.wayscompany.webhookalarm.utils.AppForeground

class WebhookAlarmApp : Application() {
    lateinit var runtime: AppRuntime
        private set

    override fun onCreate() {
        super.onCreate()
        AppForeground.install(this)
        runtime = AppRuntime(this)
    }
}
