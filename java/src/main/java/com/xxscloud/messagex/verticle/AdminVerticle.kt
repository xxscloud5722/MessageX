package com.xxscloud.messagex.verticle


import com.xxscloud.messagex.core.TokenProvider.Companion.authenticateHandler
import com.xxscloud.messagex.core.vertx.InjectorUtils
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.slf4j.LoggerFactory

class AdminVerticle : CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(AdminVerticle::class.java)
    override suspend fun start() {
        super.start()

        val module = System.getProperty("active.module") ?: ""
        if (module.isNotEmpty() && !module.contains("Admin")) {
            return
        }

        //初始化IOC
        val injector = InjectorUtils.get()
        val router = injector.getInstance(Router::class.java)

        //身份认证
        router.route("/admin/*").authenticateHandler("DEFAULT")
    }
}