package com.xxscloud.messagex.core

import com.xxscloud.messagex.core.xxs.BeanUtils
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

object JsonUtils {
    private val objectMapper: ObjectMapper = ObjectMapper()

    init {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }


    fun get(): ObjectMapper {
        return objectMapper
    }

    fun stringify(data: Any): String {
        return Json.encode(data)
    }

    fun <T> parseArray(json: String, clazz: Class<T>): List<T> {
        val result = ArrayList<T>()
        JsonArray(json).forEach {
            result.add(BeanUtils.copy(it, clazz))
        }
        return result
    }
}