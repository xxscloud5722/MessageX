package com.xxscloud.messagex.service

import com.google.inject.Inject
import com.xxscloud.messagex.dao.MessageDAO
import com.xxscloud.messagex.dao.UserDAO
import com.xxscloud.messagex.data.UserDO
import com.xxscloud.messagex.data.UserDTO
import com.xxscloud.messagex.exception.ExceptionMessageEnum
import com.xxscloud.messagex.exception.ServiceException
import org.apache.commons.beanutils.BeanUtils

class UserService @Inject constructor(private val userDAO: UserDAO, private val messageDAO: MessageDAO) {
    suspend fun registered(user: UserDO): Boolean {
        if (!userDAO.exist(user.channel, user.openId)) {
            return userDAO.insert(user)
        }
        throw ServiceException(ExceptionMessageEnum.ACCOUNT_EXIST)
    }

    suspend fun getUserInfo(id: String): UserDTO? {
        val user = userDAO.getById(id) ?: return null
        val userDTO = UserDTO()
        BeanUtils.copyProperties(userDTO, user)
        userDTO.unreadMessageCount = messageDAO.getUnreadMessageCountByUserId(id)
        return userDTO
    }


    suspend fun getUserById(id: String): UserDO? {
        return userDAO.getById(id)
    }
}