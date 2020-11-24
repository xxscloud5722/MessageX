package com.xxscloud.messagex.verticle

import com.xxscloud.messagex.core.WebSocketCore
import com.xxscloud.messagex.core.vertx.InjectorUtils
import com.xxscloud.messagex.dao.UserDAO
import io.vertx.core.http.HttpServer
import io.vertx.core.http.ServerWebSocket
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class SocketVerticle : CoroutineVerticle() {

    private val log = LoggerFactory.getLogger(SocketVerticle::class.java)

    override suspend fun start() {
        super.start()

        val module = System.getProperty("active.module") ?: ""
        if (module.isNotEmpty() && !module.contains("WebSocket")) {
            return
        }

        val injector = InjectorUtils.get()
        val server = injector.getInstance(HttpServer::class.java)
        val webSocketCore = injector.getInstance(WebSocketCore::class.java)
        val userDAO = injector.getInstance(UserDAO::class.java)

        //WebSocket
        server.webSocketHandler { socket ->
            GlobalScope.launch(vertx.dispatcher()) {
                webSocketHandler(socket, webSocketCore, userDAO)
            }
        }
        log.info("WebSocket initialization completed")
    }

    private suspend fun webSocketHandler(webSocket: ServerWebSocket, webSocketCore: WebSocketCore, userDAO: UserDAO) {
        val urls = webSocket.path().split("/")
        if (urls.size <= 1) {
            webSocket.writeTextMessage("url error ...")
            return
        }

        val token = urls[urls.size - 1]
        webSocket.writeTextMessage("Loading ...")

        val user = userDAO.getByToken(token)
        if (user == null) {
            webSocket.writeTextMessage("Token: $token error")
            webSocket.close()
            return
        }

        webSocket.writeTextMessage("Message（Push）System V1.0")
        //监听关闭事件
        webSocket.closeHandler {
            webSocketCore.remove(user.id)
        }
        //添加用户数据
        webSocketCore.add(user, webSocket)
    }

}