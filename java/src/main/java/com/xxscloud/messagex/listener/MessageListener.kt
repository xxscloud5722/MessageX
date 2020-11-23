package com.xxscloud.messagex.listener

import com.google.inject.Inject
import com.xxscloud.messagex.core.WebSocketCore
import com.xxscloud.messagex.dao.MessageDAO
import com.xxscloud.messagex.dao.MessageQueueDAO
import com.xxscloud.messagex.event.MessageEvent
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class MessageListener @Inject constructor(vertx: Vertx, private val messageDAO: MessageDAO, private val messageQueueDAO: MessageQueueDAO) {
    private val log = LoggerFactory.getLogger(MessageListener::class.java)

    init {
        vertx.eventBus().consumer<String>(MessageEvent::class.java.name) {
            GlobalScope.launch(Dispatchers.IO) {
                run(Json.decodeValue(it.body(), MessageEvent::class.java))
            }
        }
        log.info("Listener: ${MessageEvent::class.java.name}")
    }

    private suspend fun run(event: MessageEvent) {
        val message = messageDAO.getById(event.messageId) ?: return
        message.content = ""
        val userList = messageQueueDAO.getByMessageId(event.messageId)
        log.info("[消息]: 本次推送人数 ${userList.size}, 内容 ${Json.encode(message)}")
        userList.forEach {
            //WebSocketCore.sendText(it.id, "message ${Json.encode(message)}")
        }
    }
}