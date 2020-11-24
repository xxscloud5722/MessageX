package com.xxscloud.messagex.module.api


import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.xxscloud.messagex.config.Client
import com.xxscloud.messagex.config.USession
import com.xxscloud.messagex.core.vertx.InjectorUtils
import com.xxscloud.messagex.core.xxs.BeanUtils
import com.xxscloud.messagex.core.xxs.RedisCore
import com.xxscloud.messagex.exception.CoreException
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import java.util.*


abstract class BaseModule {
    companion object {
        private val objectMapper: ObjectMapper = ObjectMapper()
        private val log = LoggerFactory.getLogger(BaseModule::class.java)
        private val debug = (System.getProperties()["active"] ?: System.getenv("active") ?: "").toString().isEmpty()
         private var redisCore: RedisCore? = null

        init {
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            val module = SimpleModule()
            module.addDeserializer(Date::class.java, object : JsonDeserializer<Date>() {
                override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): Date? {
                    if (p == null) {
                        return null
                    }
                    val source = p.text.trim()
                    return BeanUtils.getValue(Date::class.java, source)
                }
            })
            objectMapper.registerModule(module)
        }


    }

    enum class ResponseType {
        JSON,
        HTML,
        IMAGE_JPEG,
        IMAGE_PNG,
        XLS
    }

    fun addRouter(route: Route, ban: Boolean = false): Route {
        return route.handler { c ->
            if (ban) {
                c.fail(CoreException("API is forbidden"))
            } else {
                c.next()
            }
        }
    }

    fun <T> getBody(content: RoutingContext, clazz: Class<T>): T {
        return objectMapper.readValue(content.bodyAsString, clazz)
    }

    fun getBody(content: RoutingContext): JsonObject {
        return content.bodyAsJson
    }


    fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Unit, time: Long = 5000L, semaphore: Semaphore? = null): Route {
        return handler { ctx ->
            h(ctx, ResponseType.JSON, if (debug) Long.MAX_VALUE else time, semaphore) {
                fn(ctx)
            }
        }
    }

    fun Route.coroutineHandler(fn: suspend (RoutingContext, USession) -> Unit, time: Long = 5000L, semaphore: Semaphore? = null): Route {
        return handler { ctx ->
            h(ctx, ResponseType.JSON, if (debug) Long.MAX_VALUE else time, semaphore) {
                fn(ctx, getSession(ctx))
            }
        }
    }

    fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Unit, type: ResponseType, time: Long = 5000L, semaphore: Semaphore? = null): Route {
        return handler { ctx ->
            h(ctx, type, if (debug) Long.MAX_VALUE else time, semaphore) {
                fn(ctx)
            }
        }
    }

    fun Route.coroutineHandler(fn: suspend (RoutingContext, USession) -> Unit, type: ResponseType, time: Long = 5000L, semaphore: Semaphore? = null): Route {
        return handler { ctx ->
            h(ctx, type, if (debug) Long.MAX_VALUE else time, semaphore) {
                fn(ctx, getSession(ctx))
            }
        }
    }


    private suspend fun getSession(context: RoutingContext): USession {
        val token = context.request().headers()["token"] ?: context.request().getParam("token") ?: ""
        if (token.isEmpty()) {
            return USession()
        }
        redisCore = if (redisCore == null) InjectorUtils.getBean(RedisCore::class.java) else redisCore
        val json = redisCore?.getString(token) ?: ""
        return if (json.isEmpty()) {
            USession()
        } else {
            Json.decodeValue(json, USession::class.java)
        }
    }

    private fun getClient(context: RoutingContext): Client {
        var ip = context.request().getHeader("x-forwarded-for") ?: ""
        if (ip.isEmpty() || ip.equals("unknown", false)) {
            ip = context.request().getHeader("Proxy-Client-IP") ?: ""
        }
        if (ip.isEmpty() || ip.equals("unknown", false)) {
            ip = context.request().getHeader("WL-Proxy-Client-IP") ?: ""
        }
        if (ip.isEmpty() || ip.equals("unknown", false)) {
            ip = context.request().getHeader("X-Real-IP") ?: ""
        }
        if (ip.isEmpty() || ip.equals("unknown", false)) {
            ip = context.request().remoteAddress().host()
        }
        if (ip.isEmpty() || ip.equals("unknown", false)) {
            ip = ""
        }
        return Client(ip,
                context.request().getHeader("Referer") ?: "",
                context.request().getHeader("User-Agent") ?: "")
    }

    private fun h(ctx: RoutingContext, type: ResponseType, time: Long, semaphore: Semaphore? = null, fn: suspend () -> Unit) {
        GlobalScope.launch(ctx.vertx().dispatcher() + CoroutineName("http") +
                CoroutineExceptionHandler { _, ex ->
                    ctx.fail(ex)
                 }) {
            withTimeout(if (debug) Long.MAX_VALUE else time) {
                addResponseType(ctx, type)
                if (semaphore != null) {
                    semaphore.withPermit {
                        fn()
                    }
                } else {
                    fn()
                }
             }
        }
    }

    private fun addResponseType(context: RoutingContext, type: ResponseType) {
        val typeString = when (type) {
            ResponseType.JSON -> {
                "application/json;charset=utf-8"
            }
            ResponseType.HTML -> {
                "text/html;charset=utf-8"
            }
            ResponseType.IMAGE_JPEG -> {
                "image/jpeg"
            }
            ResponseType.IMAGE_PNG -> {
                "image/png"
            }
            ResponseType.XLS -> {
                "application/vnd.ms-excel"
            }
            else -> {
                "text/plan;charset=utf-8"
            }
        }
        if (context.response().headers().contains("Content-Type")) {
            context.response().headers().set("Content-Type", typeString)
        } else {
            context.response().headers().add("Content-Type", typeString)
        }
    }
}
