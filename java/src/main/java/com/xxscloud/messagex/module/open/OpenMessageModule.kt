package com.xxscloud.messagex.module.open

import com.google.inject.Inject
import com.xxscloud.messagex.config.ApiResponse
import com.xxscloud.messagex.config.USession
import com.xxscloud.messagex.data.MessageDTO
import com.xxscloud.messagex.exception.ParameterException
import com.xxscloud.messagex.module.api.BaseModule
import com.xxscloud.messagex.service.OpenMessageService
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

class OpenMessageModule @Inject constructor(router: Router, private val messageService: OpenMessageService) : BaseModule() {
    init {
        addRouter(router.post("/open/message/send")).coroutineHandler(::send)
    }

    private suspend fun send(context: RoutingContext, session: USession) {
        val message = getBody(context, MessageDTO::class.java)
        if (message.title.isEmpty() || message.content.isEmpty() || message.abstract.isEmpty() ||
            (message.recipient.isNullOrEmpty() && message.recipientGroup.isNullOrEmpty())
        ) {
            throw ParameterException("参数异常")
        }
        if (message.sender.isEmpty()) {
            throw ParameterException("参数异常")
        }
        context.response().end(ApiResponse.success(messageService.send(session.id, message)).toString())
    }
}