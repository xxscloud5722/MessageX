package com.xxscloud.messagex

import com.google.inject.*
import com.xxscloud.messagex.config.ApiResponse
import com.xxscloud.messagex.core.MySQLCore
import com.xxscloud.messagex.core.RedisCore
import com.xxscloud.messagex.core.TokenProvider
import com.xxscloud.messagex.core.TokenProvider.Companion.authenticateHandler
import com.xxscloud.messagex.core.TokenProvider.Companion.checkToken
import com.xxscloud.messagex.core.WebSocketCore
import com.xxscloud.messagex.exception.*
import com.xxscloud.messagex.listener.MessageListener
import com.xxscloud.messagex.module.api.MessageModule
import com.xxscloud.messagex.module.api.UserGroupModule
import com.xxscloud.messagex.module.api.UserModule
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory


class WebApplication : CoroutineVerticle() {
    private val log = LoggerFactory.getLogger(WebApplication::class.java)
    override suspend fun start() {
        super.start()

        //初始化Redis
        val redis = RedisCore.init(vertx)
        //初始化MySQL
        val sqlCore = MySQLCore.init(vertx)

        //初始化IOC
        val injector: Injector = Guice.createInjector(object : AbstractModule() {
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

        })
        val router = injector.getInstance(Router::class.java)

        //初始化Web
        initWeb(injector, router)



        System.getProperty("server.port")?.let {
            val config = HttpServerOptions()
            config.isTcpFastOpen = true
            config.isTcpCork = true
            config.isTcpQuickAck = true
            config.isReusePort = false
            val server = vertx.createHttpServer(config)
            server.webSocketHandler {
                GlobalScope.launch(vertx.dispatcher()) {
                    webSocketHandler(it)
                }
            }
            server.requestHandler(router).listen(it.toInt())
            log.info("Server initialization completed Port：${it.toInt()}")
        }


    }

    private fun initWeb(injector: Injector, router: Router) {

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
        //读取Body
        router.route().handler(BodyHandler.create())
        //检查token
        router.route().checkToken()
        //渠道
        //router.route("/open/*").checkChannel()

        //身份认证
        router.route("/admin/*").authenticateHandler("DEFAULT")
        //全局异常
        router.route("/*").failureHandler { content ->
            content.response().headers().add("content-type", "application/json; charset=utf-8")

            val errorMessage: String?
            when (content.failure()) {
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




        injector.getInstance(UserModule::class.java)
        injector.getInstance(MessageModule::class.java)
        injector.getInstance(UserGroupModule::class.java)

        injector.getInstance(MessageListener::class.java)
    }

    private suspend fun webSocketHandler(serverWebSocket: ServerWebSocket) {
        val path = serverWebSocket.path()
        val values = path.split("/")
        if (values.size <= 1) {
            return
        }
        val token = values[values.size - 1]
        if (token.isEmpty()) {
            return
        }

        log.info("${serverWebSocket.binaryHandlerID()} ${serverWebSocket.localAddress()} 连接成功")

        //查询登陆状态
        val session = TokenProvider.check(token)
        if (session == null) {
            serverWebSocket.writeTextMessage("error token")
            serverWebSocket.close()
            return
        }


        session.webSocket = serverWebSocket
        WebSocketCore.put(session.id, session)
        serverWebSocket.writeTextMessage("ping")

        //监听消息
        serverWebSocket.textMessageHandler {
            GlobalScope.launch(vertx.dispatcher()) {
                val checkResult = TokenProvider.check(token)
                if (checkResult != null) {
                    serverWebSocket.writeTextMessage("ping")
                }
            }
        }

        //关闭连接
        serverWebSocket.closeHandler {
            WebSocketCore.remove(session.id)
            log.info("当前在线用户数: ${WebSocketCore.getCount()}")
        }
    }
}




