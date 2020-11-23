package com.xxscloud.messagex.core.xxs

import com.xxscloud.messagex.config.Config
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.awaitEvent
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.redis.client.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory

class RedisCore {
    companion object {
        private lateinit var vertx: Vertx
        private lateinit var redis: RedisAPI
        private lateinit var redisCore: RedisCore
        private val log = LoggerFactory.getLogger(RedisCore::class.java)

        @Suppress("CAST_NEVER_SUCCEEDS")
        suspend fun init(vertx: Vertx): RedisCore {
            val password = Config.getValue("redis.password")
            val port = Config.getValue("redis.port", "6379")
            val host = Config.getValue("redis.host", "127.0.0.1")
            val connectionString = "redis://${password}:@${host}:${port}/1"
            log.info("Redis Loading complete $connectionString")

            val connection = awaitResult<RedisConnection> {
                Redis.createClient(
                        vertx, RedisOptions(
                        JsonObject().put("connectionString", connectionString)
                                .put("maxPoolSize", 50)
                                .put("maxWaitingHandlers", 200)
                )
                ).connect(it)
            }

            redis = RedisAPI.api(connection)
            redisCore = RedisCore()
            Companion.vertx = vertx
            return redisCore
        }

        fun getCore(): RedisCore {
            return redisCore
        }
    }


    suspend fun getString(key: String): String {
        val result = awaitResult<Response?> { redis.get(key, it) } ?: return ""
        return result.toString()
    }

    suspend fun getInt(key: String): Int {
        val result = awaitResult<Response?> { redis.get(key, it) } ?: return 0
        return result.toInteger()
    }

    suspend fun getLong(key: String): Long {
        val result = awaitResult<Response?> { redis.get(key, it) } ?: return 0
        return result.toLong()
    }

    suspend fun getJsonObject(key: String): JsonObject {
        val result = awaitResult<Response?> { redis.get(key, it) }
        return JsonObject((result ?: "").toString())
    }

    suspend fun <T> getJsonObject(key: String, clazz: Class<T>): T? {
        val result = awaitResult<Response?> { redis.get(key, it) }
        val value = (result ?: "").toString()
        if (value.isEmpty()) {
            return null
        }
        return Json.decodeValue(value, clazz)
    }

    fun <T> getJsonObject(key: String, clazz: Class<T>, fn: (T?) -> Unit) {
        redis.get(key) { result ->
            val value = (result.result() ?: "").toString()
            if (value.isEmpty()) {
                fn(null)
                return@get
            }
            fn(Json.decodeValue(value, clazz))
            return@get
        }
    }

    suspend fun exists(key: String): Boolean {
        val result = awaitResult<Response> { redis.exists(arrayListOf(key), it) }
        return result.toString().toInt() > 0
    }

    fun exists(key: String, fn: (Boolean) -> Unit) {
        redis.exists(arrayListOf(key)) {
            fn(it.result().toString().toInt() > 0)
        }
    }


    suspend fun expire(key: String, time: Long): Boolean {
        val result = awaitResult<Response> { redis.expire(key, time.toString(), it) }
        return result.type() == ResponseType.INTEGER
    }


    suspend fun setex(key: String, time: Long, value: String): Boolean {
        val result = awaitResult<Response> { redis.setex(key, time.toString(), value, it) }
        return result.type() == ResponseType.SIMPLE
    }

    suspend fun incr(key: String): Boolean {
        val result = awaitResult<Response> { redis.incr(key, it) }
        return result.toString().toInt() > 0
    }

    suspend fun lpush(key: String, value: String): Boolean {
        val result = awaitResult<Response> { redis.lpush(listOf(key, value), it) }
        return result.toString().toInt() > 0
    }


    suspend fun llen(key: String): Int {
        val result = awaitResult<Response> { redis.llen(key, it) }
        return result.toInteger()
    }

    suspend fun lindex(key: String, index: Int): String {
        val result = awaitResult<Response> { redis.lindex(key, index.toString(), it) }
        return result.toString()
    }


    suspend fun del(key: String): Int {
        val result = awaitResult<Response> { redis.del(arrayListOf(key), it) }
        return 0
    }


    suspend fun publish(message: String): Boolean {
        val result = awaitResult<Response> { redis.publish("message", message, it) }
        return result.toBoolean()
    }

}