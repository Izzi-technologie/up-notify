package com.wayscompany.webhookalarm.websocket

import com.wayscompany.webhookalarm.data.AlertParser
import com.wayscompany.webhookalarm.model.WsIncoming
import com.wayscompany.webhookalarm.model.WsProtocol
import com.wayscompany.webhookalarm.settings.AppSettings
import com.wayscompany.webhookalarm.settings.DeviceKey
import com.wayscompany.webhookalarm.utils.AlarmLogger
import com.wayscompany.webhookalarm.utils.NoOpLogger
import com.wayscompany.webhookalarm.utils.normalizeWebSocketUrl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class WebSocketManager(
    private val factory: SocketFactory,
    private val networkOnline: StateFlow<Boolean>,
    private val scope: CoroutineScope,
    private val logger: AlarmLogger = NoOpLogger,
    private val backoff: (Int) -> Long = ReconnectBackoff::delayMs,
) {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _incoming = MutableSharedFlow<WsIncoming>(extraBufferCapacity = 32)
    val incoming: SharedFlow<WsIncoming> = _incoming.asSharedFlow()

    private val settingsRef = AtomicReference(AppSettings())
    private val pendingAck = AtomicReference<String?>(null)
    private val reconnectRequested = AtomicBoolean(false)
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val lock = Any()

    private var current: OpenSocket? = null
    private var supervisor: Job? = null

    @Volatile
    private var stopped = false

    fun start(settings: StateFlow<AppSettings>) {
        supervisor?.cancel()
        closeCurrent()
        stopped = false
        reconnectRequested.set(false)
        while (wake.tryReceive().isSuccess) Unit
        settingsRef.set(settings.value)
        supervisor = scope.launch {
            val settingsJob = launch {
                settings.collect { updated ->
                    val previous = settingsRef.getAndSet(updated)
                    val changed = updated.webSocketUrl != previous.webSocketUrl ||
                        updated.deviceId != previous.deviceId ||
                        updated.authToken != previous.authToken
                    if (changed) {
                        reconnectRequested.set(true)
                        wake.trySend(Unit)
                        closeCurrent()
                    }
                }
            }
            try {
                runLoop()
            } finally {
                settingsJob.cancel()
                closeCurrent()
            }
        }
    }

    fun reconnectNow() {
        reconnectRequested.set(true)
        wake.trySend(Unit)
        closeCurrent()
    }

    fun sendAcknowledge(alertId: String, deviceId: String) {
        val payload = WsProtocol.acknowledge(alertId, deviceId)
        if (!send(payload)) {
            pendingAck.set(payload)
            logger.w("Acknowledge queued until reconnect")
        }
    }

    private suspend fun runLoop() {
        var failures = 0
        while (currentCoroutineContext().isActive && !stopped) {
            if (!networkOnline.value) {
                closeCurrent()
                _state.value = ConnectionState.Offline
                logger.i("WebSocket offline")
                networkOnline.firstTrue()
                continue
            }
            val active = settingsRef.get()
            val url = normalizeWebSocketUrl(active.webSocketUrl)
            if (url == null) {
                _state.value = ConnectionState.Error("Invalid WebSocket URL")
                logger.e("Invalid WebSocket URL")
                failures++
                if (!waitBackoff(backoff((failures - 1).coerceAtLeast(0)), resetFailures = { failures = 0 })) break
                continue
            }
            _state.value = ConnectionState.Connecting
            logger.i("WebSocket connecting")
            val session = Session()
            val headers = buildMap {
                val token = active.authToken.trim()
                if (token.isNotEmpty()) put("Authorization", "Bearer $token")
            }
            val socket = try {
                factory.open(url, headers, sessionCallbacks(session))
            } catch (error: Throwable) {
                _state.value = ConnectionState.Error(error.message ?: "WebSocket error")
                logger.e("WebSocket error", error)
                null
            }
            if (socket == null) {
                failures++
                if (!waitBackoff(backoff((failures - 1).coerceAtLeast(0)), resetFailures = { failures = 0 })) break
                continue
            }
            synchronized(lock) { current = socket }
            coroutineScope {
                val registerJob = launch { awaitRegister(session) }
                val heartbeatJob = launch { heartbeat(session) }
                try {
                    session.closed.await()
                } finally {
                    registerJob.cancel()
                    heartbeatJob.cancel()
                }
            }
            closeCurrent()
            if (stopped) break
            if (!networkOnline.value) continue
            if (reconnectRequested.getAndSet(false)) {
                while (wake.tryReceive().isSuccess) Unit
                failures = 0
                continue
            }
            val failedOpen = session.registerTimedOut || session.registerRejected || !session.opened
            if (failedOpen) {
                failures++
                if (!waitBackoff(backoff((failures - 1).coerceAtLeast(0)), resetFailures = { failures = 0 })) break
            } else {
                failures = 0
                _state.value = ConnectionState.Disconnected
                logger.i("WebSocket disconnected")
                if (!waitBackoff(ReconnectBackoff.unexpectedCloseMs, resetFailures = { failures = 0 })) break
            }
        }
    }

    private fun sessionCallbacks(session: Session) = object : SocketCallbacks {
        override fun onOpen() {
            session.opened = true
            logger.i("WebSocket socket open")
            session.finishOpen()
        }

        override fun onMessage(text: String) {
            if (WsProtocol.isPong(text)) {
                session.notePong()
                return
            }
            val parsed = AlertParser.parse(text)
            if (parsed == null) {
                logger.w("Ignored websocket message")
                return
            }
            if (parsed is WsIncoming.Connected) {
                session.noteRegistered()
                _state.value = ConnectionState.Connected
                logger.i("WebSocket connected")
            }
            _incoming.tryEmit(parsed)
        }

        override fun onClosed() {
            session.finish()
        }

        override fun onFailure(message: String) {
            if (!session.opened) {
                _state.value = ConnectionState.Error(message)
                logger.e("WebSocket error")
            }
            session.finish()
        }
    }

    private suspend fun awaitRegister(session: Session) {
        val opened = select {
            session.openSignal.onAwait { true }
            session.closed.onAwait { false }
        }
        if (!opened || !session.opened || session.closed.isCompleted) return
        if (!sendRegister()) {
            session.registerRejected = true
            if (_state.value !is ConnectionState.Error) {
                _state.value = ConnectionState.Error("Register failed")
            }
            closeCurrent()
            return
        }
        val acked = withTimeoutOrNull(ReconnectBackoff.registerAckMs) {
            select {
                session.registered.onAwait { true }
                session.closed.onAwait { false }
            }
        }
        if (session.closed.isCompleted) return
        if (acked != true) {
            session.registerTimedOut = true
            _state.value = ConnectionState.Error("Register timed out")
            logger.w("WebSocket register timed out")
            closeCurrent()
        }
    }

    private suspend fun heartbeat(session: Session) {
        val registered = select {
            session.registered.onAwait { true }
            session.closed.onAwait { false }
        }
        if (!registered || session.closed.isCompleted) return
        while (currentCoroutineContext().isActive && !session.closed.isCompleted) {
            while (session.pongs.tryReceive().isSuccess) Unit
            if (!send(WsProtocol.ping())) {
                logger.w("WebSocket ping failed")
                closeCurrent()
                return
            }
            val pong = withTimeoutOrNull(ReconnectBackoff.pongTimeoutMs) {
                session.pongs.receive()
            }
            if (session.closed.isCompleted) return
            if (pong == null) {
                logger.w("WebSocket pong timeout")
                closeCurrent()
                return
            }
            delay(ReconnectBackoff.pingIntervalMs)
        }
    }

    private fun sendRegister(): Boolean {
        val deviceId = settingsRef.get().deviceId.trim()
        if (!DeviceKey.isValid(deviceId)) {
            _state.value = ConnectionState.Error("Invalid device key")
            logger.e("Invalid device key")
            return false
        }
        val sent = send(WsProtocol.register(deviceId))
        if (sent) flushPending()
        return sent
    }

    private fun flushPending() {
        val payload = pendingAck.get() ?: return
        if (send(payload)) pendingAck.compareAndSet(payload, null)
    }

    private fun send(text: String): Boolean {
        val socket = synchronized(lock) { current }
        return try {
            socket?.send(text) == true
        } catch (error: Throwable) {
            logger.e("WebSocket send failed", error)
            false
        }
    }

    private fun closeCurrent() {
        val socket = synchronized(lock) {
            val open = current
            current = null
            open
        }
        try {
            socket?.close()
        } catch (error: Throwable) {
            logger.e("WebSocket close failed", error)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun waitBackoff(delayMs: Long, resetFailures: () -> Unit): Boolean {
        if (stopped) return false
        if (reconnectRequested.getAndSet(false)) {
            while (wake.tryReceive().isSuccess) Unit
            resetFailures()
            return true
        }
        val woke = select {
            onTimeout(delayMs) { false }
            wake.onReceive { true }
        }
        if (stopped) return false
        if (woke || reconnectRequested.getAndSet(false)) {
            while (wake.tryReceive().isSuccess) Unit
            resetFailures()
        }
        return true
    }

    private suspend fun StateFlow<Boolean>.firstTrue() {
        if (value) return
        first { it }
    }

    private class Session {
        val openSignal = CompletableDeferred<Unit>()
        val registered = CompletableDeferred<Unit>()
        val closed = CompletableDeferred<Unit>()
        val pongs = Channel<Unit>(Channel.CONFLATED)

        @Volatile
        var opened: Boolean = false

        @Volatile
        var registerTimedOut: Boolean = false

        @Volatile
        var registerRejected: Boolean = false

        fun finishOpen() {
            if (!openSignal.isCompleted) openSignal.complete(Unit)
        }

        fun noteRegistered() {
            if (!registered.isCompleted) registered.complete(Unit)
        }

        fun notePong() {
            pongs.trySend(Unit)
        }

        fun finish() {
            finishOpen()
            if (!closed.isCompleted) closed.complete(Unit)
        }
    }
}
