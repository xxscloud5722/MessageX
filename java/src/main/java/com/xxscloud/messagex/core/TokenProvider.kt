package com.xxscloud.messagex.core

import com.xxscloud.messagex.config.Config
import com.xxscloud.messagex.config.USession
import com.xxscloud.messagex.core.xxs.MySQLCore
import com.xxscloud.messagex.core.xxs.RedisCore
import com.xxscloud.messagex.data.UserDO
import com.xxscloud.messagex.exception.CoreException
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import io.vertx.ext.web.Route
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.codec.digest.DigestUtils

class TokenProvider {
    companion object {
        private val TOKEN_PROVIDER = TokenProvider()
        private val F = arrayListOf(
                "/",
                "/favicon.ico",

                "/upload",


                "/admin/system/login",
                "/admin/system/upload",
        )
        private val Q = arrayListOf("/open/")

        fun Route.authenticateHandler(role: String): Route {
            return handler { ctx ->
                GlobalScope.launch(ctx.vertx().dispatcher()) {
                    val user = awaitResult<User> { TOKEN_PROVIDER.authenticate(null, it) }
                    if (user.principal().getJsonArray("role").contains(role)) {
                        ctx.next()
                    } else {
                        ctx.fail(401, CoreException("401\nUnauthorized"))
                    }
                }
            }
        }

        fun Route.checkToken(): Route {
            return handler { ctx ->
                GlobalScope.launch(ctx.vertx().dispatcher()) {
                    //如果过滤的地址
                    if (F.contains(ctx.request().path())) {
                        ctx.next()
                        return@launch
                    }
                    val flag = Q.find { ctx.request().path().startsWith(it) }
                    if (flag != null) {
                        ctx.next()
                        return@launch
                    }
                    val token = ctx.request().headers()["token"] ?: ctx.request().getParam("token")
                    //如果没有token直接返回
                    if (token.isNullOrEmpty()) {
                        ctx.fail(CoreException("401\ntoken is null"))
                        return@launch
                    }

                    val session = check(token)
                    when {
                        session == null -> {
                            ctx.fail(500, CoreException("401\ntoken expired"))
                            return@launch
                        }
                        session.status != 1 -> {
                            ctx.fail(500, CoreException("401\n您的账号已冻结"))
                            return@launch
                        }
                        else -> {
                            ctx.request().headers().add("user-id", session.id)
                            ctx.next()
                            return@launch
                        }
                    }
                }
            }
        }


        private suspend fun check(token: String): USession? {
            //如果有token 则去数据库查询
            val tokens = token.split("_")
            if (tokens.size <= 1) {
                return null
            }


            //查询redis
            if (RedisCore.getCore().exists(token)) {
                RedisCore.getCore().expire(token, Config.R_T_30M)
                return RedisCore.getCore().getJsonObject(token, USession::class.java)
            }

            //查询数据库
            val session = USession()

            when (tokens[0]) {
//                "ADMIN" -> {
//                    val user = MySQLCore.getCore().queryFirst(
//                            """
//                        SELECT * FROM system_admin_user WHERE token = ?
//                    """,
//                            arrayListOf(token),
//                            AdminUserDO::class.java
//                    )
//                    user?.let {
//                        session.id = user.id
//                        session.account = user.account
//                        session.token = user.token
//                        RedisCore.getCore().setex(token, Config.R_T_30M, Json.encode(session))
//                    }
//                }
                "API" -> {
                    val user = MySQLCore.getCore().queryFirst(
                            """
                        SELECT * FROM e_user WHERE token = ?
                    """,
                            arrayListOf(token),
                            UserDO::class.java
                    )
                    user?.let {
                        session.id = user.id
                        session.token = user.token
                        session.status = user.status
                        RedisCore.getCore().setex(token, Config.R_T_30M, Json.encode(session))
                    }
                }
            }
            return if (session.id.isEmpty()) null else session
        }
    }


    class TokenProvider : AuthProvider {
        override fun authenticate(authInfo: JsonObject?, resultHandler: Handler<AsyncResult<User>>?) {
            resultHandler?.handle(Future.succeededFuture(TokenUser()))
        }

        class TokenUser : User {
            override fun clearCache(): User {
                return this
            }

            override fun setAuthProvider(authProvider: AuthProvider?) {
            }

            override fun isAuthorized(authority: String?, resultHandler: Handler<AsyncResult<Boolean>>?): User {
                return this
            }

            override fun principal(): JsonObject {
                val role = JsonArray()
                role.add("DEFAULT")

                val result = JsonObject()
                result.put("role", role)
                return result
            }
        }
    }
}