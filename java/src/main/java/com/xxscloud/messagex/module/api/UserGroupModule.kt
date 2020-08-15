package com.xxscloud.messagex.module.api

import com.google.inject.Inject
import com.xxscloud.messagex.config.ApiResponse
import com.xxscloud.messagex.data.UserGroupDO
import com.xxscloud.messagex.data.UserGroupDTO
import com.xxscloud.messagex.exception.ParameterException
import com.xxscloud.messagex.module.BaseModule
import com.xxscloud.messagex.service.UserGroupService
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.TimeoutHandler

class UserGroupModule @Inject constructor(router: Router, private val userGroupService: UserGroupService) : BaseModule() {
    init {
        addRouter(router.post("/open/userGroup/create")).coroutineHandler(5000L,::create)
        addRouter(router.post("/open/userGroup/getGroupInfo")).coroutineHandler(5000L,::getGroupInfo)
        addRouter(router.post("/open/userGroup/getGroupList")).coroutineHandler(5000L,::getGroupList)
        addRouter(router.post("/open/userGroup/joinGroup")).coroutineHandler(5000L,::joinGroup)
    }

    private suspend fun create(content: RoutingContext) {
        val userGroup = getBody(content, UserGroupDO::class.java)
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

    private suspend fun getGroupList(content: RoutingContext) {
        content.response().end(ApiResponse.success(userGroupService.getGroupList()).toString())
    }

    private suspend fun joinGroup(content: RoutingContext) {
        val userGroup = getBody(content, UserGroupDTO::class.java)
        if (userGroup.id.isEmpty() || userGroup.userId.isEmpty()) {
            throw ParameterException("参数异常")
        }
        content.response().end(ApiResponse.success(userGroupService.joinGroup(userGroup.id, userGroup.userId)).toString())
    }
}