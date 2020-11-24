package com.xxscloud.messagex.module.open

import com.google.inject.Inject
import com.xxscloud.messagex.config.ApiResponse
import com.xxscloud.messagex.config.USession
import com.xxscloud.messagex.data.UserGroupDO
import com.xxscloud.messagex.data.UserGroupDTO
import com.xxscloud.messagex.exception.ParameterException
import com.xxscloud.messagex.module.api.BaseModule
import com.xxscloud.messagex.service.UserGroupService
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

class OpenUserGroupModule @Inject constructor(router: Router, private val userGroupService: UserGroupService) : BaseModule() {
    init {
        addRouter(router.post("/open/userGroup/create")).coroutineHandler(::create)
        addRouter(router.post("/open/userGroup/getGroupInfo")).coroutineHandler(::getGroupInfo)
        addRouter(router.post("/open/userGroup/getGroupList")).coroutineHandler(::getGroupList)
        addRouter(router.post("/open/userGroup/joinGroup")).coroutineHandler(::joinGroup)
    }

    private suspend fun create(content: RoutingContext, session: USession) {
        val userGroup = getBody(content, UserGroupDO::class.java)
        userGroup.channelId = session.id
        if (userGroup.name.isEmpty()) {
            throw ParameterException("参数异常")
        }
        content.response().end(ApiResponse.success(userGroupService.create(userGroup)).toString())
    }

    private suspend fun getGroupInfo(content: RoutingContext) {
        val userGroup = getBody(content, UserGroupDO::class.java)
        if (userGroup.id.isEmpty()) {
            throw ParameterException("参数异常")
        }
        content.response().end(ApiResponse.success(userGroupService.getGroupInfo(userGroup.id)).toString())
    }

    private suspend fun getGroupList(content: RoutingContext, session: USession) {
        content.response().end(ApiResponse.success(userGroupService.getGroupList(session.id)).toString())
    }

    private suspend fun joinGroup(content: RoutingContext) {
        val userGroup = getBody(content, UserGroupDTO::class.java)
        if (userGroup.id.isEmpty() || userGroup.users == null) {
            throw ParameterException("参数异常")
        }
        content.response().end(ApiResponse.success(
            userGroupService.joinGroup(userGroup.id, userGroup.users!!)).toString())
    }
}