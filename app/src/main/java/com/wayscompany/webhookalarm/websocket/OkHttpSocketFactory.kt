package com.wayscompany.webhookalarm.websocket

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class OkHttpSocketFactory(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build(),
) : SocketFactory {
    override fun open(url: String, headers: Map<String, String>, callbacks: SocketCallbacks): OpenSocket {
        val request = Request.Builder().url(url).apply {
            headers.forEach { (name, value) -> header(name, value) }
        }.build()
        val socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                callbacks.onOpen()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                callbacks.onMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                callbacks.onClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                callbacks.onFailure(t.message ?: "WebSocket failure")
            }
        })
        return OkHttpOpenSocket(socket)
    }
}

private class OkHttpOpenSocket(private val socket: WebSocket) : OpenSocket {
    override fun send(text: String): Boolean = socket.send(text)

    override fun close() {
        socket.close(1000, "close")
    }
}
