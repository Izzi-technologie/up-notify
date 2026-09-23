package com.wayscompany.webhookalarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wayscompany.webhookalarm.AppRuntime
import com.wayscompany.webhookalarm.model.Severity
import com.wayscompany.webhookalarm.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppScreen {
    Home,
    Settings,
}

class MainViewModel(private val runtime: AppRuntime) : ViewModel() {
    val settings = runtime.settings
    val loaded = runtime.loaded
    val connection = runtime.connectionState
    val alert = runtime.alertState
    val lastEvent = runtime.lastEvent

    private val _screen = MutableStateFlow(AppScreen.Home)
    val screen = _screen.asStateFlow()

    fun connect(serverUrl: String, deviceId: String) {
        viewModelScope.launch {
            runtime.saveConnection(serverUrl, deviceId)
            runtime.ensureStarted()
            runtime.reconnectNow()
        }
    }

    fun continueSetup() {
        viewModelScope.launch { runtime.completeSetup() }
    }

    fun openSettings() {
        _screen.value = AppScreen.Settings
    }

    fun closeSettings() {
        _screen.value = AppScreen.Home
    }

    fun acknowledge() {
        runtime.acknowledge()
    }

    fun test(severity: Severity) {
        runtime.test(severity)
    }

    fun save(settings: AppSettings) {
        viewModelScope.launch { runtime.save(settings) }
    }

    fun testConnection(settings: AppSettings) {
        viewModelScope.launch {
            runtime.save(settings)
            runtime.reconnectNow()
        }
    }

    fun testCritical(settings: AppSettings) {
        viewModelScope.launch {
            runtime.save(settings)
            runtime.test(Severity.CRITICAL)
        }
    }

    fun restartService(settings: AppSettings) {
        viewModelScope.launch {
            runtime.save(settings)
            runtime.restartService()
        }
    }

    companion object {
        fun factory(runtime: AppRuntime): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MainViewModel(runtime) as T
            }
    }
}
