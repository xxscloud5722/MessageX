package com.xxscloud.messagex.module

import com.google.gson.GsonBuilder
import com.xxscloud.messagex.config.USession
import com.xxscloud.messagex.core.RedisCore
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory


abstract class BaseModule {
    companion object {
        private val log = LoggerFactory.getLogger(BaseModule::class.java)
        private val GSON = GsonBuilder().create()
        private val debug = (System.getProperties()["active"] ?: System.getenv("active") ?: "").toString().isEmpty()
    }


    fun addRouter(route: Route): Route {
        return route.handler { c ->
            GlobalScope.launch(c.vertx().dispatcher()) {
                c.response().headers().add("content-type", "application/json; charset=utf-8")
                c.next()
            }
        }
    }

    fun <T> getBody(content: RoutingContext, clazz: Class<T>): T {
        val result = GSON.fromJson(content.bodyAsString, clazz)
        return result ?: clazz.newInstance()
    }

    fun getBody(content: RoutingContext): JsonObject {
        return content.bodyAsJson
    }

    fun Route.coroutineHandler(time: Long = 5000L, fn: suspend (RoutingContext) -> Unit): Route {
        return handler { ctx ->
            GlobalScope.launch(ctx.vertx().dispatcher()) {
                val startTime = System.currentTimeMillis()
                withTimeout(if (debug) Long.MAX_VALUE else time) {
                    try {
                        fn(ctx)
                    } catch (e: Exception) {
                        ctx.fail(e)
                    } catch (e: Throwable) {
                        ctx.fail(e)
                    }
                }
                log.info("[request] ${ctx.request().path()} time: ${System.currentTimeMillis() - startTime}")
            }
        }
    }

    fun Route.coroutineHandler(time: Long = 5000L, fn: suspend (RoutingContext, USession) -> Unit): Route {
        return handler { ctx ->
            GlobalScope.launch(ctx.vertx().dispatcher()) {
                val startTime = System.currentTimeMillis()
                withTimeout(if (debug) Long.MAX_VALUE else time) {
                    try {
                        val token = ctx.request().getHeader("token") ?: ""
                        val tokens = token.split("_")
                        val session =
                            if (tokens.size > 1) RedisCore.getCore().getJsonObject(tokens[1], USession::class.java)
                            else null
                        fn(ctx, session ?: USession())
                    } catch (e: Exception) {
                        ctx.fail(e)
                    } catch (e: Throwable) {
                        ctx.fail(e)
                    }
                }
                log.info("[request] ${ctx.request().path()} time: ${System.currentTimeMillis() - startTime}")
            }
        }
    }


}