package com.xxscloud.messagex.core.xxs

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.math.BigDecimal
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object BeanUtils {

    fun <T> copy(obj: Any, clazz: Class<T>): T {
        return when (obj) {
            is JsonObject -> {
                copyMapToBean(obj.map, clazz)
            }
            is Map<*, *> -> {
                copyMapToBean(obj, clazz)
            }
            else -> {
                copyBeanToBean(obj, clazz)
            }
        }
    }

    private fun <T> copyBeanToBean(obj: Any, clazz: Class<T>): T {
        val constructor = clazz.getConstructor()
        val data = constructor.newInstance()
        val getClazz = obj.javaClass
        clazz.declaredFields.forEach { field ->
            val item = clazz.declaredFields.find { it.name == field.name } ?: return@forEach

            //读取方法
            val getName = "get" + item.name.substring(0, 1).toUpperCase() + item.name.substring(1)
            val getMethod = getClazz.methods.find { m -> m.name == getName } ?: return@forEach

            //设置方法
            val setName = "set" + item.name.substring(0, 1).toUpperCase() + item.name.substring(1)
            val setMethod = clazz.methods.find { m -> m.name == setName } ?: return@forEach
            val flag = getMethod.invoke(obj)
            if (flag != null) {
                setMethod.invoke(data, getValue(item.type, flag))
            }
        }
        return data
    }

    private fun <T> copyMapToBean(obj: Map<*, *>, clazz: Class<T>): T {
        val constructor = clazz.getConstructor()
        val data = constructor.newInstance()
        clazz.declaredFields.forEach { field ->
            //读取数据
            val result = obj[field.name] ?: return@forEach
            //设置方法
            val setName = "set" + field.name.substring(0, 1).toUpperCase() + field.name.substring(1)
            val setMethod = clazz.methods.find { m -> m.name == setName } ?: return@forEach
            setMethod.invoke(data, getValue(field.type, result))
        }
        return data
    }

    fun toMap(document: Any): HashMap<String, Any> {
        val result = HashMap<String, Any>()
        val clazz = document::class.java
        clazz.declaredFields.forEach {
            val name = "get" + it.name.substring(0, 1).toUpperCase() + it.name.substring(1)
            val method = clazz.getMethod(name) ?: return@forEach
            val value = method.invoke(document)
            if (value is ArrayList<*>) {
                result[it.name] = toArrayMap(value)
            } else {
                result[it.name] = value
            }
        }
        return result
    }

    fun toArrayMap(document: List<Any>): ArrayList<Any> {
        val result = ArrayList<Any>()
        document.forEach {
            if (it is ArrayList<*>) {
                result.add(toArrayMap(it))
            } else {
                result.add(toMap(it))
            }
        }
        return result
    }


    fun toJsonObject(document: Any): JsonObject {
        val result = JsonObject()
        val clazz = document::class.java
        clazz.declaredFields.forEach {
            val name = "get" + it.name.substring(0, 1).toUpperCase() + it.name.substring(1)
            val method = clazz.getMethod(name) ?: return@forEach
            when (val value = method.invoke(document)) {
                is ArrayList<*> -> {
                    result.put(it.name, toJsonArray(value))
                }
                is Date -> {
                    result.put(it.name, value.time)
                }
                is String -> {
                    result.put(it.name, value)
                }
                is BigInteger -> {
                    result.put(it.name, value.toLong())
                }
                is BigDecimal -> {
                    result.put(it.name, value.toDouble())
                }
                is Number -> {
                    result.put(it.name, value)
                }
                is Comparable<*> -> {
                    result.put(it.name, value.toString())
                }
                else -> {
                    result.put(it.name, toMap(value))
                }
            }
        }
        return result
    }

    fun toJsonArray(document: List<Any>): JsonArray {
        val result = JsonArray()
        document.forEach {
            if (it is ArrayList<*>) {
                result.add(toJsonArray(it))
            } else {
                result.add(toJsonObject(it))
            }
        }
        return result
    }

    fun <T> jsonObjectToBean(value: JsonObject, clazz: Class<T>): T? {
        val constructor = clazz.getConstructor()
        val data = constructor.newInstance()
        clazz.declaredFields.forEach {
            if (value.getValue(it.name) == null) {
                return@forEach
            }
            val name = "set" + it.name.substring(0, 1).toUpperCase() + it.name.substring(1)
            val method = clazz.methods.find { m -> m.name == name } ?: return@forEach
            method.invoke(data, getValue(it.type, value.getValue(it.name)))
        }
        return data
    }


    @Suppress("UNCHECKED_CAST")
    fun <T> toBean(columnNames: List<String>, row: JsonArray, clazz: Class<T>): T? {
        when (clazz) {
            JsonObject::class.java -> {
                val result = JsonObject()
                columnNames.forEachIndexed { i, name ->
                    result.put(name, row.getValue(i))
                }
                return result as T
            }
            String::class.java -> {
                return getValue(clazz, row, 0) as T
            }
            else -> {
                val constructor = clazz.getConstructor()
                val data = constructor.newInstance()
                clazz.methods.forEach { m ->
                    if (!m.name.startsWith("set")) {
                        return@forEach
                    }
                    val field = m.name[3].toLowerCase() + m.name.substring(4)
                    //字段名称
                    var index = columnNames.indexOf(field)
                    //小写
                    if (index < 0) {
                        index = columnNames.indexOf(field.toLowerCase())
                    }
                    //驼峰转下划线
                    if (index < 0) {
                        index = columnNames.indexOf(toDbName(field))
                    }
                    if (index <= -1) {
                        return@forEach
                    }
                    //获取结果, 如果是null 什么都不处理
                    val r = getValue(m.parameterTypes[0], row, index)
                    r?.let {
                        m.invoke(data, r)
                    }
                }
                return data
            }
        }
    }

    fun <T> getValue(clazz: Class<T>, row: JsonArray, index: Int): T? {
        val flag = row.getValue(index)
        if (flag == null || flag.toString().isEmpty()) {
            return null
        }
        return getValue(clazz, flag)
//        return when (clazz) {
//            Date::class.java ->
//                if (row.getValue(index) == null || row.getValue(index).toString().isEmpty()) null
//               else Date(row.getInstant(index).toEpochMilli()) as T?
//            Int::class.java, java.lang.Integer::class.java ->
//                if (row.getValue(index) == null || row.getValue(index).toString().isEmpty()) null else row.getInteger(index) as T?
//            Long::class.java, java.lang.Long::class.java ->
//                if (row.getValue(index) == null || row.getValue(index).toString().isEmpty()) null else row.getLong(index) as T?
//            Double::class.java, java.lang.Double::class.java ->
//                if (row.getValue(index) == null || row.getValue(index).toString().isEmpty()) null else row.getDouble(index) as T?
//            Float::class.java, java.lang.Float::class.java ->
//                if (row.getValue(index) == null || row.getValue(index).toString().isEmpty()) null else row.getFloat(index) as T?
//            BigDecimal::class.java ->
//                if (row.getValue(index) == null || row.getValue(index).toString().isEmpty()) null else BigDecimal.valueOf(row.getDouble(index)) as T?
//            String::class.java ->
//                if (row.getValue(index) == null || row.getValue(index).toString().isEmpty()) null else row.getValue(index).toString() as T?
//            Boolean::class.java, java.lang.Boolean::class.java ->
//                if (row.getValue(index) == null || row.getValue(index).toString().isEmpty()) null else
//                    (row.getValue(index).toString().toLowerCase() == "true" || row.getValue(index).toString() == "1") as T?
//            else -> null
//        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getValue(clazz: Class<T>, value: Any): T {
        if (value.javaClass == clazz) {
            return value as T
        }
        return when (clazz) {
            Date::class.java -> {
                val source = value.toString().trim()
                when {
                    source.matches("^\\d{4}-\\d{1,2}$".toRegex()) -> {
                        return SimpleDateFormat("yyyy-MM").parse(source) as T
                    }
                    source.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$".toRegex()) -> {
                        return SimpleDateFormat("yyyy-MM-dd").parse(source) as T
                    }
                    source.matches("^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}$".toRegex()) -> {
                        return SimpleDateFormat("yyyy-MM-dd hh:mm").parse(source) as T
                    }
                    source.matches("^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}:\\d{1,2}$".toRegex()) -> {
                        return SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(source) as T
                    }
                    source.matches("^\\d{4}-\\d{1,2}-\\d{1,2}T{1}\\d{1,2}:\\d{1,2}:\\d{1,2}Z{1}\$".toRegex()) -> {
                        return Date(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(source)).toEpochMilli()) as T
                    }
                    source.matches("^\\d{13}$".toRegex()) -> {
                        return Date(source.toLong()) as T
                    }
                    source.matches("^\\d{10}$".toRegex()) -> {
                        return Date(source.toLong() * 1000) as T
                    }
                    else -> {
                        throw  IllegalArgumentException("Invalid Date value '$source'")
                    }
                }
            }
            Int::class.java, java.lang.Integer::class.java ->
                value.toString().toInt() as T
            Long::class.java, java.lang.Long::class.java ->
                value.toString().toLong() as T
            Double::class.java, java.lang.Double::class.java ->
                value.toString().toDouble() as T
            Float::class.java, java.lang.Float::class.java ->
                value.toString().toFloat() as T
            BigDecimal::class.java ->
                value.toString().toBigDecimal() as T
            BigInteger::class.java ->
                value.toString().toBigInteger() as T
            String::class.java ->
                value.toString() as T
            Boolean::class.java, java.lang.Boolean::class.java -> {
                if (value.toString().length == 1) {
                    value.toString().toInt() == 1
                } else {
                    value.toString().toBoolean()
                } as T
            }
            else -> throw IllegalArgumentException("Unrecognized type")
        }
    }

    private fun toDbName(name: String): String {
        val value = StringBuilder()
        name.forEach {
            if (it in 'A'..'Z') {
                value.append("_").append(it.toLowerCase())
            } else {
                value.append(it)
            }
        }
        return value.toString()
    }

}