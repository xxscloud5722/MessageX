package com.xxscloud.messagex.service

import com.google.inject.Inject
import com.xxscloud.messagex.core.xxs.BeanUtils
import com.xxscloud.messagex.dao.MessageDAO
import com.xxscloud.messagex.dao.UserDAO
import com.xxscloud.messagex.data.UserDO
import com.xxscloud.messagex.data.UserDTO
import com.xxscloud.messagex.exception.ExceptionMessageEnum
import com.xxscloud.messagex.exception.ServiceException
import org.apache.commons.codec.digest.DigestUtils
import java.util.*

class UserService @Inject constructor(private val userDAO: UserDAO, private val messageDAO: MessageDAO) {
    suspend fun registered(user: UserDO): Boolean {
        val openId = DigestUtils.md5Hex(user.channel + user.openId)
        if (userDAO.exist(openId)) {
            throw ServiceException(ExceptionMessageEnum.ACCOUNT_EXIST)
        }
        user.openId = openId
        return userDAO.insert(user)
    }

    suspend fun getUserInfo(id: String): UserDTO? {
        val user = userDAO.getById(id) ?: return null
        val userDTO = BeanUtils.copy(user, UserDTO::class.java)
        userDTO.unreadMessageCount = messageDAO.getUnreadMessageCountByUserId(id)
        userDTO.token = ""
        return userDTO
    }


    suspend fun generateToken(id: String): String {
        val token = "API_" + UUID.randomUUID().toString().replace("-", "")
        if (!userDAO.updateToken(id, token)) {
            throw ServiceException(ExceptionMessageEnum.TOKEN_FAILURE)
        }
        return token
    }
}