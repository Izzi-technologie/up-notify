package com.wayscompany.webhookalarm.utils

import android.util.Log

interface AlarmLogger {
    fun i(message: String)
    fun w(message: String)
    fun e(message: String, throwable: Throwable? = null)
}

object AndroidAlarmLogger : AlarmLogger {
    private const val TAG = "IzziWebhookAlarm"

    override fun i(message: String) {
        Log.i(TAG, message)
    }

    override fun w(message: String) {
        Log.w(TAG, message)
    }

    override fun e(message: String, throwable: Throwable?) {
        if (throwable == null) Log.e(TAG, message) else Log.e(TAG, message, throwable)
    }
}

object NoOpLogger : AlarmLogger {
    override fun i(message: String) = Unit
    override fun w(message: String) = Unit
    override fun e(message: String, throwable: Throwable?) = Unit
}
