package com.xxscloud.messagex.service

import com.google.inject.Inject
import com.xxscloud.messagex.core.xxs.BeanUtils
import com.xxscloud.messagex.dao.UserGroupDAO
import com.xxscloud.messagex.data.UserGroupDO
import com.xxscloud.messagex.data.UserGroupDTO
import com.xxscloud.messagex.exception.ExceptionMessageEnum
import com.xxscloud.messagex.exception.ServiceException

class UserGroupService @Inject constructor(private val userGroupDAO: UserGroupDAO) {
    suspend fun create(userGroup: UserGroupDO): Boolean {
        return userGroupDAO.insert(userGroup)
    }

    suspend fun getGroupInfo(id: String): UserGroupDTO? {
        val group = userGroupDAO.getById(id) ?: throw ServiceException(ExceptionMessageEnum.DATA_NULL)
        val userGroup = BeanUtils.copy(group, UserGroupDTO::class.java)
        userGroup.userList = userGroupDAO.getUserList(id)
        return userGroup
    }

    suspend fun getGroupList(channelId: String): List<UserGroupDO> {
        return userGroupDAO.getGroupAllList(channelId)
    }

    suspend fun joinGroup(groupId: String, users: List<String>): Boolean {
        users.forEach {
            if (!userGroupDAO.exist(groupId, it)) {
                userGroupDAO.joinGroup(groupId, it)
            }
        }
        return true
    }
}