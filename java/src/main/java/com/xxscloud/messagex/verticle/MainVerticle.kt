package com.xxscloud.messagex.verticle


import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Provides
import com.google.inject.Singleton
import com.xxscloud.messagex.core.vertx.InjectorUtils
import com.xxscloud.messagex.core.xxs.MySQLCore
import com.xxscloud.messagex.core.xxs.RedisCore
import com.xxscloud.messagex.listener.MessageListener
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*


class MainVerticle : CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(MainVerticle::class.java)

    override suspend fun start() {
        super.start()

        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"))
        log.info("当前系统时间: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()))
        log.info("当前系统时区: " + TimeZone.getDefault().displayName)

        //加载配置文件
        loadConfig()

        //初始化Redis
        val redis = RedisCore.init(vertx)
        //初始化MySQL
        val sqlCore = MySQLCore.init(vertx)


        //初始化容器
        val injector = InjectorUtils.factory {
            Guice.createInjector(object : AbstractModule() {
                override fun configure() {

                }

                @Provides
                @Singleton
                fun getRouter(): Router {
                    return Router.router(vertx)
                }


                @Provides
                @Singleton
                fun getMySQL(): MySQLCore {
                    return sqlCore
                }

                @Provides
                @Singleton
                fun getRedis(): RedisCore {
                    return redis
                }


                @Provides
                @Singleton
                fun getVertx(): Vertx {
                    return vertx
                }

                @Provides
                @Singleton
                fun getHttpServer(): HttpServer {
                    val config = HttpServerOptions()
                    config.isTcpFastOpen = true
                    config.isTcpCork = true
                    config.isTcpQuickAck = true
                    config.isReusePort = false
                    return vertx.createHttpServer(config)
                }
            })
        }

        //监听器
        injector.getInstance(MessageListener::class.java)

        log.info("Injector 加载完成 ..")


        //装载Web 服务
        vertx.deployVerticle(WebApplication())
    }

    private fun loadConfig() {
        val active = (System.getProperties()["active"] ?: System.getenv("active") ?: "").toString()
        log.info("[当前环境配置]: $active")
        val configPath = "/application${if (active.isEmpty()) "" else "-$active"}.properties";
        val properties = Properties()
        properties.load(this.javaClass.getResourceAsStream(configPath))
        log.info("Config: $properties")
        properties.forEach {
            System.setProperty(it.key.toString(), it.value.toString())
        }
        //设置模块
        if ((System.getProperty("active.module") ?: "").isEmpty()) {
            System.setProperty("active.module", System.getenv("ACTIVE_MODULE") ?: "")
        }
        //-Dlogback.configurationFile=logback-pro.xml 指定日志
    }
}