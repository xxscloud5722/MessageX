package com.xxscloud.messagex.service

import com.google.inject.Inject
import com.xxscloud.messagex.dao.UserGroupDAO
import com.xxscloud.messagex.data.UserGroupDO
import com.xxscloud.messagex.data.UserGroupDTO
import org.apache.commons.beanutils.BeanUtils

class UserGroupService @Inject constructor(private val userGroupDAO: UserGroupDAO) {
    suspend fun create(userGroup: UserGroupDO): Boolean {
        return userGroupDAO.insert(userGroup)
    }

    suspend fun getGroupInfo(id: String): UserGroupDTO? {
        val userGroup = UserGroupDTO()
        val group = userGroupDAO.getById(id)
        BeanUtils.copyProperties(group, userGroup)
        userGroup.userList = userGroupDAO.getUserList(id)
        return userGroup
    }

    suspend fun getGroupList(): List<UserGroupDO> {
        return userGroupDAO.getGroupAllList()
    }

    suspend fun joinGroup(groupId: String, userId: String): Boolean {
        if (userGroupDAO.exist(groupId, userId)) {
            return true
        }
        return userGroupDAO.joinGroup(groupId, userId)
    }
}