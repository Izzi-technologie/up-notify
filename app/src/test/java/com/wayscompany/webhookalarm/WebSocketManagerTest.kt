package com.wayscompany.webhookalarm

import com.wayscompany.webhookalarm.model.WsProtocol
import com.wayscompany.webhookalarm.settings.AppSettings
import com.wayscompany.webhookalarm.websocket.ConnectionState
import com.wayscompany.webhookalarm.websocket.OpenSocket
import com.wayscompany.webhookalarm.websocket.ReconnectBackoff
import com.wayscompany.webhookalarm.websocket.SocketCallbacks
import com.wayscompany.webhookalarm.websocket.SocketFactory
import com.wayscompany.webhookalarm.websocket.WebSocketManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketManagerTest {
    @Test
    fun backoffSequenceThenStaysAtSixtySeconds() {
        assertEquals(5_000L, ReconnectBackoff.delayMs(0))
        assertEquals(10_000L, ReconnectBackoff.delayMs(1))
        assertEquals(30_000L, ReconnectBackoff.delayMs(2))
        assertEquals(60_000L, ReconnectBackoff.delayMs(3))
        assertEquals(60_000L, ReconnectBackoff.delayMs(9))
    }

    @Test
    fun reconnectsWithBackoffAndNeverOverlapsSockets() = runTest {
        val factory = FakeSocketFactory(failImmediately = true)
        val manager = manager(factory, online = true)
        manager.start(settings())
        runCurrent()
        assertEquals(1, factory.created)
        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(1, factory.created)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, factory.created)
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(3, factory.created)
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(4, factory.created)
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(5, factory.created)
        assertEquals(0, factory.maxLive)
    }

    @Test
    fun keepsASingleSocketWhileConnectedAndSendsRegister() = runTest {
        val factory = FakeSocketFactory(failImmediately = false)
        val manager = manager(factory, online = true)
        manager.start(settings())
        runCurrent()
        assertEquals(ConnectionState.Connected, manager.state.value)
        assertEquals(1, factory.created)
        assertEquals(1, factory.maxLive)
        advanceTimeBy(120_000)
        runCurrent()
        assertEquals(1, factory.created)
        assertTrue(factory.sent.any { it.contains("\"type\":\"register\"") && it.contains("device-001") })
        assertTrue(WsProtocol.register("device-001").contains("device-001"))
    }

    @Test
    fun staysOfflineUntilNetworkReturns() = runTest {
        val online = MutableStateFlow(false)
        val factory = FakeSocketFactory(failImmediately = false)
        val manager = WebSocketManager(factory, online, backgroundScope)
        manager.start(settings())
        runCurrent()
        assertEquals(ConnectionState.Offline, manager.state.value)
        assertEquals(0, factory.created)
        online.value = true
        runCurrent()
        assertEquals(1, factory.created)
        assertEquals(1, factory.maxLive)
    }

    private fun settings() = MutableStateFlow(
        AppSettings(
            deviceId = "device-001",
            webSocketUrl = "wss://alarm.example.com/ws",
            setupCompleted = true,
        ),
    )

    private fun TestScope.manager(factory: FakeSocketFactory, online: Boolean) =
        WebSocketManager(factory, MutableStateFlow(online), backgroundScope)
}

private class FakeSocketFactory(private val failImmediately: Boolean) : SocketFactory {
    var created = 0
    var live = 0
    var maxLive = 0
    val sent = mutableListOf<String>()

    override fun open(url: String, headers: Map<String, String>, callbacks: SocketCallbacks): OpenSocket {
        check(live == 0) { "A second WebSocket was opened" }
        created += 1
        val socket = FakeSocket(callbacks)
        if (failImmediately) {
            callbacks.onFailure("boom")
        } else {
            live = 1
            maxLive = maxOf(maxLive, live)
            callbacks.onOpen()
        }
        return socket
    }

    private inner class FakeSocket(private val callbacks: SocketCallbacks) : OpenSocket {
        private var closed = false

        override fun send(text: String): Boolean {
            sent += text
            return true
        }

        override fun close() {
            if (closed) return
            closed = true
            if (live > 0) live -= 1
            callbacks.onClosed()
        }
    }
}
