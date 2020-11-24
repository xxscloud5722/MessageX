package com.xxscloud.messagex.module.api

import com.google.inject.Inject
import com.xxscloud.messagex.config.ApiResponse
import com.xxscloud.messagex.config.USession
import com.xxscloud.messagex.data.MessageDO
import com.xxscloud.messagex.data.MessageDTO
import com.xxscloud.messagex.exception.ParameterException
import com.xxscloud.messagex.service.MessageService
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

class MessageModule @Inject constructor(router: Router, private val messageService: MessageService) : BaseModule() {

    init {
        addRouter(router.post("/message/getContent")).coroutineHandler(::getContent)
        addRouter(router.post("/message/getMessageList")).coroutineHandler(::getMessageList)
        addRouter(router.post("/message/markMessage")).coroutineHandler(::markMessage)
    }

    private suspend fun getContent(context: RoutingContext) {
        val message = getBody(context, MessageDO::class.java)
        if (message.id.isEmpty()) {
            throw ParameterException("参数异常")
        }
        context.response().end(ApiResponse.success(messageService.getContent(message.id)).toString())
    }

    private suspend fun getMessageList(context: RoutingContext, session: USession) {
        val message = getBody(context, MessageDTO::class.java)
        if (message.id.isEmpty()) {
            message.id = "0"
        }
        context.response().end(ApiResponse.success(messageService.getMessageList(session.id, message.id, message.status)).toString())
    }

    private suspend fun markMessage(context: RoutingContext, session: USession) {
        val message = getBody(context, MessageDO::class.java)
        if (message.id.isEmpty()) {
            throw ParameterException("参数异常")
        }
        context.response().end(ApiResponse.success(messageService.markMessage(session.id, message.id)).toString())
    }
}