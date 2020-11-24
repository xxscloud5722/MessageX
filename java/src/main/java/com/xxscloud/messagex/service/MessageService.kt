package com.xxscloud.messagex.service

import com.google.inject.Inject
import com.xxscloud.messagex.core.xxs.MySQLCore
import com.xxscloud.messagex.dao.MessageDAO
import com.xxscloud.messagex.dao.MessageQueueDAO
import com.xxscloud.messagex.dao.UserGroupDAO
import com.xxscloud.messagex.data.MessageDO
import com.xxscloud.messagex.data.MessageDTO
import com.xxscloud.messagex.event.MessageEvent
import com.xxscloud.messagex.exception.ExceptionMessageEnum
import com.xxscloud.messagex.exception.ServiceException
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import org.apache.commons.beanutils.BeanUtils

class MessageService @Inject constructor(
    private val messageDAO: MessageDAO,
    private val messageQueueDAO: MessageQueueDAO
) {


    suspend fun getContent(id: String): MessageDO? {
        return messageDAO.getById(id)
    }

    suspend fun getMessageList(userId: String, messageId: String, status: Int): List<MessageDTO> {
        return messageDAO.getMessageList(userId, messageId, status)
    }

    suspend fun markMessage(userId: String, messageId: String): Boolean {
        return messageQueueDAO.markMessage(userId, messageId)
    }
}