package com.xxscloud.messagex.module.api


import com.google.inject.Inject
import com.xxscloud.messagex.config.ApiResponse
import com.xxscloud.messagex.config.USession
import com.xxscloud.messagex.data.UserDO
import com.xxscloud.messagex.exception.ParameterException
import com.xxscloud.messagex.service.UserService
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext


class UserModule @Inject constructor(router: Router, private val userService: UserService) : BaseModule() {


    init {
        addRouter(router.post("/open/user/registered")).coroutineHandler( ::registered)

        addRouter(router.post("/user/getUserInfo")).coroutineHandler( ::getUserInfo)
        addRouter(router.post("/user/checkToken")).coroutineHandler( ::checkToken)
    }

    private suspend fun registered(content: RoutingContext) {
        val user = getBody(content, UserDO::class.java)
        user.channel = content.request().getHeader("x-code") ?: ""
        if (user.openId.isEmpty() || user.nickName.isEmpty() || user.openId.length > 32) {
            throw ParameterException("参数异常")
        }
        content.response().end(ApiResponse.success(userService.registered(user)).toString())
    }

    private suspend fun getUserInfo(content: RoutingContext, session: USession) {
        content.response().end(ApiResponse.success(userService.getUserInfo(session.id)).toString())
    }

    private suspend fun checkToken(content: RoutingContext, session: USession) {
        content.response().end(ApiResponse.success(userService.getUserById(session.id)).toString())
    }
}