package com.xxscloud.messagex.service

import com.google.inject.Inject
import com.xxscloud.messagex.core.xxs.BeanUtils
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

class OpenMessageService @Inject constructor(
    private val messageDAO: MessageDAO,
    private val messageQueueDAO: MessageQueueDAO,
    private val userGroupDAO: UserGroupDAO
) {
    suspend fun send(message: MessageDTO, vertx: Vertx): MessageDO {
        val messageDO = BeanUtils.copy(message, MessageDO::class.java)
        //写入数据库
        var flag = false
        MySQLCore.transaction { tx ->
            if (!messageDAO.insert(messageDO, tx)) {
                throw ServiceException(ExceptionMessageEnum.SAVE_ERROR)
            }
            if (message.recipient.isNullOrEmpty()) {
                //收件人
                message.recipient?.let { messageList ->
                    flag = true
                    for (recipient in messageList) {
                        if (!messageQueueDAO.insert(messageDO.id, recipient, tx)) {
                            throw ServiceException(ExceptionMessageEnum.SAVE_ERROR)
                        }
                    }
                }
            } else {
                //用户组
                message.recipientGroup?.let { messageList ->
                    for (recipient in messageList) {
                        val userList = userGroupDAO.getUserList(recipient, tx)
                        flag = if (!flag) userList.isNotEmpty() else flag
                        for (user in userList) {
                            if (!messageQueueDAO.insert(messageDO.id, user.id, tx)) {
                                throw ServiceException(ExceptionMessageEnum.SAVE_ERROR)
                            }
                        }
                    }
                }
            }
        }

        //通知事件处理
        if (flag) {
            vertx.eventBus().publish(MessageEvent::class.java.name, Json.encode(MessageEvent(messageDO.id)))
        }
        return messageDO
    }
}