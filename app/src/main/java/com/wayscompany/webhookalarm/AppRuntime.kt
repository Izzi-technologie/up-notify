package com.wayscompany.webhookalarm

import android.content.Context
import android.content.Intent
import com.wayscompany.webhookalarm.alarm.AlertEngine
import com.wayscompany.webhookalarm.audio.AlarmAudioPlayer
import com.wayscompany.webhookalarm.model.Severity
import com.wayscompany.webhookalarm.model.WsIncoming
import com.wayscompany.webhookalarm.service.AlarmForegroundService
import com.wayscompany.webhookalarm.settings.AppSettings
import com.wayscompany.webhookalarm.settings.SettingsRepository
import com.wayscompany.webhookalarm.utils.AndroidAlarmLogger
import com.wayscompany.webhookalarm.utils.deriveWebSocketUrl
import com.wayscompany.webhookalarm.websocket.AndroidNetworkStatus
import com.wayscompany.webhookalarm.websocket.OkHttpSocketFactory
import com.wayscompany.webhookalarm.websocket.WebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppRuntime(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val logger = AndroidAlarmLogger
    private val connectionRequested = MutableStateFlow(false)
    private var started = false

    val settingsRepository = SettingsRepository(appContext, scope)
    val network = AndroidNetworkStatus(appContext)
    val isAudioActive = MutableStateFlow(false)
    val audio = AlarmAudioPlayer(appContext, logger) { active -> isAudioActive.value = active }
    val webSocket = WebSocketManager(
        factory = OkHttpSocketFactory(),
        networkOnline = network.online,
        scope = scope,
        logger = logger,
    )
    val engine = AlertEngine(
        audio = audio,
        scope = scope,
        policies = { settingsRepository.settings.value.policies },
        onAcknowledge = { alertId ->
            webSocket.sendAcknowledge(alertId, settingsRepository.settings.value.deviceId)
        },
        logger = logger,
    )

    val settings = settingsRepository.settings
    val loaded = settingsRepository.loaded
    val connectionState = webSocket.state
    val alertState = engine.state
    val lastEvent = engine.lastEvent

    fun ensureStarted() {
        if (started) return
        started = true
        scope.launch {
            loaded.first { it }
            combine(settings, connectionRequested) { current, requested ->
                current.setupCompleted || requested
            }.first { it }
            webSocket.start(settings)
            webSocket.incoming.collect { message ->
                when (message) {
                    is WsIncoming.Alert -> engine.onAlert(message.event)
                    is WsIncoming.Resolved -> engine.onResolved(message.id)
                    is WsIncoming.Connected -> Unit
                }
            }
        }
    }

    suspend fun save(settings: AppSettings) {
        settingsRepository.save(settings)
    }

    suspend fun saveConnection(serverUrl: String, deviceId: String, authToken: String) {
        settingsRepository.update { current ->
            current.copy(
                serverUrl = serverUrl.trim(),
                deviceId = deviceId.trim(),
                webSocketUrl = deriveWebSocketUrl(serverUrl),
                authToken = authToken.trim(),
            )
        }
        connectionRequested.value = true
    }

    suspend fun completeSetup() {
        settingsRepository.update { it.copy(setupCompleted = true) }
    }

    fun acknowledge() {
        engine.acknowledge()
    }

    fun dismiss() {
        engine.dismiss()
    }

    fun test(severity: Severity) {
        engine.test(severity)
    }

    fun reconnectNow() {
        connectionRequested.value = true
        webSocket.reconnectNow()
    }

    fun restartService() {
        logger.i("Alarm service restarted")
        appContext.stopService(Intent(appContext, AlarmForegroundService::class.java))
        AlarmForegroundService.start(appContext)
        webSocket.reconnectNow()
    }
}
