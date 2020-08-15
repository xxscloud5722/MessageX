package com.xxscloud.messagex.core

import com.xxscloud.messagex.config.Config
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.redis.client.*
import org.slf4j.LoggerFactory

class RedisCore {
    companion object {
        private lateinit var redis: RedisAPI
        private lateinit var redisCore: RedisCore
        private val log = LoggerFactory.getLogger(RedisCore::class.java)
        suspend fun init(vertx: Vertx): RedisCore {
            val password = Config.parseEnv(System.getProperty("redis.password")) ?: ""
            val port = (Config.parseEnv(System.getProperty("redis.port")) ?: "6379").toInt()
            val host = Config.parseEnv(System.getProperty("redis.host")) ?: ""
            val connectionString = "redis://${password}:@${host}:${port}/1"
            val connection = awaitResult<RedisConnection> {
                Redis.createClient(
                    vertx, RedisOptions(
                        JsonObject().put("connectionString", connectionString)
                            .put("maxPoolSize", 50)
                            .put("maxWaitingHandlers", 200)
                    )
                ).connect(it)
            }
            log.info("Redis Loading complete $connectionString")
            redis = RedisAPI.api(connection)
            redisCore = RedisCore()
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
        if(value.isEmpty()){
            return null
        }
        return Json.decodeValue(value, clazz)
    }

    suspend fun exists(key: String): Boolean {
        val result = awaitResult<Response> { redis.exists(arrayListOf(key), it) }
        return result.toString().toInt() > 0
    }

    suspend fun expire(key: String, time: Long): Boolean {
        val result = awaitResult<Response> { redis.expire(key, time.toString(), it) }
        return result.type() == ResponseType.INTEGER
    }

    suspend fun setex(key: String, time: Long, value: String): Boolean {
        val result = awaitResult<Response> { redis.setex(key, time.toString(), value, it) }
        return result.type() == ResponseType.SIMPLE
    }
}