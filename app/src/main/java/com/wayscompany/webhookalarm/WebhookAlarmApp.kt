package com.wayscompany.webhookalarm

import android.app.Application

class WebhookAlarmApp : Application() {
    lateinit var runtime: AppRuntime
        private set

    override fun onCreate() {
        super.onCreate()
        runtime = AppRuntime(this)
    }
}
