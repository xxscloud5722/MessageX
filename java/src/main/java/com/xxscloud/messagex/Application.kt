package com.xxscloud.messagex

import com.xxscloud.messagex.core.vertx.VertUtils
import com.xxscloud.messagex.verticle.MainVerticle
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.*

class Application {
    companion object {
        private val VERT_X: Vertx = VertUtils.factory()

        @JvmStatic
        fun main(vararg args: String) {
            VERT_X.deployVerticle(MainVerticle())
        }
    }
}