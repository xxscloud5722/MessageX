package com.xxscloud.messagex.core

import com.xxscloud.messagex.config.Config
import com.xxscloud.messagex.config.USession
import com.xxscloud.messagex.data.ChannelDO
import com.xxscloud.messagex.data.UserDO
import com.xxscloud.messagex.exception.CoreException
import com.xxscloud.messagex.exception.ParameterException
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
        private val F = arrayListOf("/user/login")

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
                    val token = ctx.request().headers()["token"]
                    //如果没有token直接返回
                    if (token.isNullOrEmpty()) {
                        ctx.next()
                        return@launch
                    }

                    //如果过滤的地址
                    if (F.contains(ctx.request().path())) {
                        return@launch
                    }

                    val session = check(token)
                    if (session == null) {
                        ctx.fail(500, CoreException("401\ntoken error"))
                        return@launch
                    } else {
                        ctx.next()
                        return@launch
                    }
                }
            }
        }

        fun Route.checkChannel(): Route {
            return handler { ctx ->
                GlobalScope.launch(ctx.vertx().dispatcher()) {
                    val code = ctx.request().headers()["x-code"]
                    val sign = ctx.request().headers()["x-sign"]
                    val timestamp = ctx.request().headers()["x-timestamp"]
                    //如果没有token直接返回
                    if (code.isNullOrEmpty() || sign.isNullOrEmpty() || timestamp.isNullOrEmpty()) {
                        ctx.fail(401, ParameterException("401\nx-code,x-sign,x-timestamp error"))
                        return@launch
                    }

                    //查询数据库获取数据
                    val channel = MySQLCore.getCore().queryFirst(
                        """
                            SELECT * FROM m_channel WHERE code = ?
                        """, arrayListOf(code), ChannelDO::class.java
                    )
                    //如果查询不到渠道
                    if (channel == null) {
                        ctx.fail(401, CoreException("401\nchannel error"))
                        return@launch
                    }

                    //如果签名不正确
                    if (DigestUtils.md5Hex(code + timestamp + channel.key) != sign) {
                        ctx.fail(401, CoreException("401\nsign error"))
                        return@launch
                    }

                    //继续执行
                    ctx.next()
                    return@launch
                }
            }
        }



        suspend fun check(token: String): USession? {
            //如果有token 则去数据库查询
            val tokens = token.split("_")
            if (tokens.size <= 1) {
                return null
            }

            //查询redis
            if (RedisCore.getCore().exists(tokens[1])) {
                RedisCore.getCore().expire(tokens[1], Config.R_T_30M)
                return RedisCore.getCore().getJsonObject(tokens[1], USession::class.java)
            }

            //查询数据库
            val session = USession()
            val user = MySQLCore.getCore().queryFirst(
                """
                        SELECT * FROM m_user WHERE token = ?
                    """, arrayListOf(tokens[1]), UserDO::class.java
            )
            user?.let {
                session.id = user.id
                session.account = user.account
                session.token = user.token
                RedisCore.getCore().setex(tokens[1], Config.R_T_30M, Json.encode(session))
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