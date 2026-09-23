package com.wayscompany.webhookalarm.websocket

interface SocketCallbacks {
    fun onOpen()
    fun onMessage(text: String)
    fun onClosed()
    fun onFailure(message: String)
}

interface OpenSocket {
    fun send(text: String): Boolean
    fun close()
}

interface SocketFactory {
    fun open(url: String, headers: Map<String, String>, callbacks: SocketCallbacks): OpenSocket
}
