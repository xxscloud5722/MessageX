package com.xxscloud.messagex.core.vertx

import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject

object VertUtils {
    fun factory(): Vertx {
        return Vertx.vertx(VertxOptions(JsonObject().put("setWorkerPoolSize", "30").put("setInternalBlockingPoolSize", "30")))
    }
}