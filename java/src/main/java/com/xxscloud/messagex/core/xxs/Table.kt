package com.xxscloud.messagex.core.xxs

import io.vertx.core.json.JsonObject

data class Table<T>(
    val currentIndex: Int = 1,
    val totalCount: Long = 0,
    val rows: List<T>? = null,
    val pageSize: Int = 50,
    val args: HashMap<String, Any> = HashMap()
) {
    val data: JsonObject
        get() = JsonObject(args)
    val limit: Int
        get() = (currentIndex - 1) * pageSize
}