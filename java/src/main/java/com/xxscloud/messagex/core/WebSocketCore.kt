package com.xxscloud.messagex.core

import com.xxscloud.messagex.config.USession
import java.util.concurrent.ConcurrentHashMap

object WebSocketCore {
    private val map = ConcurrentHashMap<String, USession>()

    fun put(id: String, session: USession) {
        map[id] = session
    }

    fun remove(id: String) {
        map.remove(id)
    }

    fun getCount(): Int {
        return map.size
    }

    fun sendText(id: String, value: String) {
        map[id]?.webSocket?.writeTextMessage(value)
    }
}