package com.xxscloud.messagex.module.open


import com.google.inject.Inject
import com.xxscloud.messagex.config.ApiResponse
import com.xxscloud.messagex.config.USession
import com.xxscloud.messagex.data.UserDO
import com.xxscloud.messagex.exception.ParameterException
import com.xxscloud.messagex.module.api.BaseModule
import com.xxscloud.messagex.service.UserService
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext


class OpenUserModule @Inject constructor(router: Router, private val userService: UserService) : BaseModule() {

    init {
        addRouter(router.post("/open/user/registered")).coroutineHandler(::registered)
        addRouter(router.post("/open/user/getUserInfo")).coroutineHandler(::getUserInfo)
        addRouter(router.post("/open/user/generateToken")).coroutineHandler(::generateToken)
    }

    private suspend fun registered(content: RoutingContext, session: USession) {
        val user = getBody(content, UserDO::class.java)
        user.channel = session.id
        if (user.openId.isEmpty() || user.nickName.isEmpty()) {
            throw ParameterException("参数异常")
        }
        content.response().end(ApiResponse.success(userService.registered(user)).toString())
    }

    private suspend fun getUserInfo(content: RoutingContext) {
        val user = getBody(content, UserDO::class.java)
        if (user.id.isEmpty()) {
            throw ParameterException("参数异常")
        }
        content.response().end(ApiResponse.success(userService.getUserInfo(user.id)).toString())
    }

    private suspend fun generateToken(content: RoutingContext) {
        val user = getBody(content, UserDO::class.java)
        if (user.id.isEmpty()) {
            throw ParameterException("参数异常")
        }
        content.response().end(ApiResponse.success(userService.generateToken(user.id)).toString())
    }
}