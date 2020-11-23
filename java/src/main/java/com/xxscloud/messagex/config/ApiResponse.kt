package com.xxscloud.messagex.config


import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*


data class ApiResponse(
        val success: Boolean,
        val data: Any? = null,
        val code: String? = null,
        val message: String? = null,
        val other: Any? = null
) {
    companion object {
        private val objectMapper: ObjectMapper = ObjectMapper()

        init {
            val module = SimpleModule()
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            module.addSerializer(JsonObject::class.java, object : JsonSerializer<JsonObject>() {
                override fun serialize(value: JsonObject?, gen: JsonGenerator?, serializers: SerializerProvider?) {
                    if (value?.map?.size ?: 0 > 0) {
                        gen?.writeObject(value?.map)
                    } else {
                        gen?.writeNull()
                    }
                }
            })
            module.addSerializer(JsonArray::class.java, object : JsonSerializer<JsonArray>() {
                override fun serialize(value: JsonArray?, gen: JsonGenerator?, serializers: SerializerProvider?) {
                    if (value?.list?.size ?: 0 > 0) {
                        gen?.writeObject(value?.list)
                    } else {
                        gen?.writeNull()
                    }
                }
            })
            module.addSerializer(Date::class.java, object : JsonSerializer<Date>() {
                override fun serialize(value: Date?, gen: JsonGenerator?, serializers: SerializerProvider?) {
                    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    gen?.writeObject(formatter.format(value))
                }
            })
            module.addSerializer(BigDecimal::class.java, object : JsonSerializer<BigDecimal>() {
                override fun serialize(value: BigDecimal?, gen: JsonGenerator?, serializers: SerializerProvider?) {
                    val formatter = DecimalFormat("#0.00000000")
                    gen?.writeObject(formatter.format(value))
                }
            })
            module.addSerializer(Double::class.java, object : JsonSerializer<Double>() {
                override fun serialize(value: Double?, gen: JsonGenerator?, serializers: SerializerProvider?) {
                    val formatter = DecimalFormat("#0.00000000")
                    gen?.writeObject(formatter.format(value))
                }
            })
            objectMapper.registerModule(module)
        }


        fun error(message: String): ApiResponse {
            return ApiResponse(false, null, "9999", message)
        }

        fun error(code: String, message: String?): ApiResponse {
            return ApiResponse(false, null, code, message)
        }

        fun success(): ApiResponse {
            return ApiResponse(true, null, "200", null)
        }

        fun success(data: Any?): ApiResponse {
            return ApiResponse(true, data, "200", null)
        }

        fun success(data: Any?, other: Any?): ApiResponse {
            return ApiResponse(true, data, "200", null, other)
        }

        fun success(data: Any?, message: String?): ApiResponse {
            return ApiResponse(true, data, "200", message)
        }


    }

    override fun toString(): String {
        return objectMapper.writeValueAsString(this)
    }

}