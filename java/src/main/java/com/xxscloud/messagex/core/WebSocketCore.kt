package com.xxscloud.messagex.core

import com.xxscloud.messagex.config.USession
import com.google.inject.Singleton
import com.xxscloud.messagex.data.UserDO
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.ServerWebSocket
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPOutputStream


@Singleton
class WebSocketCore {

    companion object {
        private val userList = ConcurrentHashMap<String, USession>()
        private val log = LoggerFactory.getLogger(WebSocketCore::class.java)
    }

    fun get(): ConcurrentHashMap<String, USession> {
        return userList
    }

    fun send(key: String, data: String = "") {
        userList[key]?.let {
            val bos = ByteArrayOutputStream()
            val gzip = GZIPOutputStream(bos)
            OutputStreamWriter(gzip, StandardCharsets.UTF_8).use { writer ->
                writer.write(data)
            }
            it.webSocket?.writeBinaryMessage(Buffer.buffer(bos.toByteArray()))
        }
    }

    fun remove(key: String) {
        log.info("[Socket] 用户 $key 离开房间 ..")
        userList.remove(key)
    }

    fun add(user: UserDO, webSocket: ServerWebSocket) {
        val item = userList[user.id]
        //如果存在就关闭连接
        if (item != null) {
            item.webSocket?.close()
        }
        log.info("[Socket] 用户 ${user.id} 进来了 ..")
        userList[user.id] = USession(id = user.id, ip = webSocket.localAddress().toString(), webSocket = webSocket)
    }
}