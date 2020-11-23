package com.xxscloud.messagex.verticle


import com.xxscloud.messagex.core.vertx.InjectorUtils
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.slf4j.LoggerFactory

class ApiVerticle : CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(ApiVerticle::class.java)
    override suspend fun start() {
        super.start()

        val module = System.getProperty("active.module") ?: ""
        if (module.isNotEmpty() && !module.contains("Api")) {
            return
        }

        //初始化IOC
        val injector = InjectorUtils.get()

        //Api模块
    }
}