package com.xxscloud.messagex.verticle



import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.google.inject.*
import com.xxscloud.messagex.config.ApiResponse
import com.xxscloud.messagex.core.TokenProvider.Companion.checkToken
import com.xxscloud.messagex.core.vertx.InjectorUtils
import com.xxscloud.messagex.exception.*
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import kotlinx.coroutines.TimeoutCancellationException
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*


class WebApplication : CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(WebApplication::class.java)


    override suspend fun start() {
        super.start()
        //初始化IOC
        val injector = InjectorUtils.get()

        //初始化Web
        loadDefaultWeb(injector)


        awaitResult<String> { vertx.deployVerticle(AdminVerticle(), it) }
        awaitResult<String> { vertx.deployVerticle(ApiVerticle(), it) }
        awaitResult<String> { vertx.deployVerticle(SocketVerticle(), it) }

        //开始监听
        System.getProperty("server.port")?.let {
            val server = injector.getInstance(HttpServer::class.java)
            val httpServer = server.requestHandler(injector.getInstance(Router::class.java))
            httpServer.listen(it.toInt())
            log.info("Server initialization completed Port：${it.toInt()}")
        }
    }

    private fun loadDefaultWeb(injector: Injector) {
        val router = injector.getInstance(Router::class.java)

        //正则默认读取body, 排除upload 接口
        router.route().handler {
            if (it.request().path().endsWith("/upload")) {
                it.next()
            } else {
                BodyHandler.create().handle(it)
            }
        }

        //默认页面
        router.get("/").handler { context ->
            val html = """
                <h2>API Server 1.0</h2>
                <p>Data: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())}</p>
                <p>Web Netty 1.0.1</p>
                <hr/>
                <p>时间顺流而下，生活逆水行舟</p>
            """
            context.response().headers().add("content-type", "text/html; charset=UTF-8")
            context.response().end(html)
        }

        //默认头部
        router.route().handler {
            it.response().headers().add("Access-Control-Allow-Origin", "*")
            it.response().headers().add("Access-Control-Allow-Credentials", "true")
            it.response().headers().add("Access-Control-Allow-Methods", "*")
            it.response().headers().add("Access-Control-Allow-Headers", "x-requested-with,content-type,key,token")
            it.next()
        }

        //options预处理
        router.options().handler {
            it.response().end()
        }

        //全局异常
        router.route().failureHandler { content ->
            content.response().headers().add("content-type", "application/json; charset=utf-8")

            val errorMessage: String?
            when (content.failure()) {
                is JsonParseException, is MismatchedInputException -> {
                    errorMessage = "Json 解析异常"
                }
                is ParameterException -> {
                    errorMessage = content.failure().localizedMessage
                }
                is ServiceException, is ThirdpartyException, is CoreException, is EventException -> {
                    errorMessage = content.failure().localizedMessage
                    log.error(content.request().path(), content.failure())
                }
                is TimeoutCancellationException -> {
                    errorMessage = "坐下来喝杯咖啡，稍后再试~"
                    log.error(content.request().path(), content.failure())
                }
                else -> {
                    errorMessage = null
                    log.error(content.request().path(), content.failure())
                }
            }

            val message = (errorMessage ?: "糟糕，服务器飞到火星去了").split("\n")
            content.response().statusCode = 200
            content.response().end(
                    (if (message.size > 1)
                        ApiResponse.error(message[0], message[1])
                    else ApiResponse.error("500", message[0])).toString()
            )
            content.next()
        }


        //检查token
        router.route().checkToken()
    }

}




