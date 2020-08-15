package com.xxscloud.messagex.core


import com.xxscloud.messagex.config.Config
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.ResultSet
import io.vertx.ext.sql.SQLConnection
import io.vertx.ext.sql.UpdateResult
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.ext.sql.commitAwait
import io.vertx.kotlin.ext.sql.getConnectionAwait
import io.vertx.kotlin.ext.sql.rollbackAwait
import io.vertx.kotlin.ext.sql.setAutoCommitAwait
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*
import kotlin.collections.ArrayList


class MySQLCore {

    companion object {
        private lateinit var vertx: Vertx
        private lateinit var client: JDBCClient
        private lateinit var sqlCore: MySQLCore
        private var statistics: Boolean = false
        private val log = LoggerFactory.getLogger(MySQLCore::class.java)
        fun init(vertx: Vertx): MySQLCore {

            statistics = (Config.parseEnv(System.getProperty("mysql.statistics")) ?: "0").toInt() > 0

            val host = Config.parseEnv(System.getProperty("mysql.host")) ?: ""
            val port = (Config.parseEnv(System.getProperty("mysql.port")) ?: "3306").toInt()
            val database = Config.parseEnv(System.getProperty("mysql.database")) ?: ""
            val user = Config.parseEnv(System.getProperty("mysql.user")) ?: ""
            val password = Config.parseEnv(System.getProperty("mysql.password")) ?: ""


            MySQLDataSourceProvider.init(
                JsonObject()
                    .put(
                        "url",
                        "jdbc:mysql://${host}:${port}/${database}?useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true&useSSL=false&serverTimezone=GMT%2B8"
                    )
                    .put("user", user)
                    .put("password", password)
                    .put("maxActive", "500")
                    .put("minIdle", "5")
                    .put("initialSize", "3")
            )
            val client = JDBCClient.createShared(vertx, JsonObject().put("provider_class", "com.xxscloud.messagex.core.MySQLDataSourceProvider"))


            log.info("MySQL Loading complete Host: $host Port: $port Database: $database")
            this.client = client
            this.sqlCore = MySQLCore()
            this.vertx = vertx
            return sqlCore
        }

        fun getCore(): MySQLCore {
            return sqlCore
        }

        suspend fun transaction(fn: suspend (SQLConnection?) -> Unit) {
            val startTime = if (statistics) System.currentTimeMillis() else 0L
            val connection = client.getConnectionAwait()
            connection.setAutoCommitAwait(false)
            try {
                fn(connection)
                connection.commitAwait()
            } finally {
                connection.rollbackAwait()
                connection.close()
                if (statistics) {
                    log.info("[MySQL] execution transaction total time: ${System.currentTimeMillis() - startTime} ms")
                }
            }
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

    private fun <T> toBean(columnNames: List<String>, row: JsonArray, clazz: Class<T>): T? {
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


    @Suppress("UNCHECKED_CAST")
    private fun <T> getValue(clazz: Class<T>, row: JsonArray, index: Int): T? {
        return when (clazz) {
            Date::class.java -> SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(row.getString(index)) as T?
            Int::class.java -> row.getInteger(index) as T?
            Long::class.java -> row.getLong(index) as T?
            Double::class.java -> row.getDouble(index) as T?
            Float::class.java -> row.getFloat(index) as T?
            BigDecimal::class.java -> BigDecimal.valueOf(row.getDouble(index)) as T?
            String::class.java -> row.getValue(index).toString() as T?
            Boolean::class.java -> row.getBoolean(index) as T?
            else -> null
        }
    }

    @Suppress("DuplicatedCode")
    private suspend fun <T> execution(sqlConnection: SQLConnection?, fn: suspend (SQLConnection) -> T?): T? {
        val startTime = if (statistics) System.currentTimeMillis() else 0L
        if (statistics) {
            log.info("[MySQL] getConnection Start ...")
        }
        val connection = sqlConnection ?: awaitResult { client.getConnection(it) }
        try {
            return fn(connection)
        } finally {
            if (statistics) {
                log.info("[MySQL] execution sql: ${System.currentTimeMillis() - startTime} ms")
            }
            if (sqlConnection == null) {
                connection.close()
            }
        }
    }

    @Suppress("DuplicatedCode")
    private suspend fun <T> executionResultList(sqlConnection: SQLConnection?, fn: suspend (SQLConnection) -> List<T>): List<T> {
        val startTime = if (statistics) System.currentTimeMillis() else 0L
        if (statistics) {
            log.info("[MySQL] getConnection Start ...")
        }
        val connection = sqlConnection ?: awaitResult { client.getConnection(it) }
        try {
            return fn(connection)
        } finally {
            if (statistics) {
                log.info("[MySQL] execution sql: ${System.currentTimeMillis() - startTime} ms")
            }
            if (sqlConnection == null) {
                connection.close()
            }
        }
    }


    suspend fun <T> queryResult(sql: String, tuple: List<Any>, clazz: Class<T>, transaction: SQLConnection?): T? {
        val executeTuple = JsonArray()
        tuple.forEach {
            executeTuple.add(it)
        }
        log.debug("queryResult : $sql")
        return execution(transaction) { connection ->
            val result = awaitResult<JsonArray> {
                if (tuple.isNotEmpty()) {
                    connection.querySingleWithParams(sql, executeTuple, it)
                } else {
                    connection.querySingle(sql, it)
                }
            }
            if (result.size() > 0) {
                return@execution getValue(clazz, result, 0)
            }
            return@execution null
        }
    }

    suspend fun <T> queryResult(sql: String, tuple: List<Any>, clazz: Class<T>): T? {
        return this.queryResult(sql, tuple, clazz, null)
    }

    suspend fun <T> queryResult(sql: String, clazz: Class<T>, transaction: SQLConnection?): T? {
        return this.queryResult(sql, ArrayList(), clazz, transaction)
    }

    suspend fun <T> queryResult(sql: String, clazz: Class<T>): T? {
        return this.queryResult(sql, ArrayList(), clazz, null)
    }


    suspend fun <T> queryFirst(sql: String, tuple: List<Any>, clazz: Class<T>, transaction: SQLConnection?): T? {
        val executeTuple = JsonArray()
        tuple.forEach {
            executeTuple.add(it)
        }
        log.debug("queryFirst $sql")
        return execution(transaction) { connection ->
            val result = awaitResult<ResultSet> {
                if (tuple.isNotEmpty()) {
                    connection.queryWithParams(sql, executeTuple, it)
                } else {
                    connection.query(sql, it)
                }
            }
            if (result.results.size > 0) {
                return@execution toBean(result.columnNames, result.results[0], clazz)
            }
            return@execution null
        }
    }

    suspend fun <T> queryFirst(sql: String, tuple: List<Any>, clazz: Class<T>): T? {
        return this.queryFirst(sql, tuple, clazz, null)
    }

    suspend fun <T> queryFirst(sql: String, clazz: Class<T>, transaction: SQLConnection?): T? {
        return this.queryFirst(sql, ArrayList(), clazz, transaction)
    }

    suspend fun <T> queryFirst(sql: String, clazz: Class<T>): T? {
        return this.queryFirst(sql, ArrayList(), clazz, null)
    }


    suspend fun <T> query(sql: String, tuple: List<Any>, clazz: Class<T>, transaction: SQLConnection?): List<T> {
        val executeTuple = JsonArray()
        tuple.forEach {
            executeTuple.add(it)
        }
        log.debug("query $sql")
        return executionResultList(transaction) { connection ->
            val result = awaitResult<ResultSet> {
                if (tuple.isNotEmpty()) {
                    connection.queryWithParams(sql, executeTuple, it)
                } else {
                    connection.query(sql, it)
                }
            }
            if (result.results.size > 0) {
                val resultList = ArrayList<T>()
                result.results.forEach {
                    val value = toBean(result.columnNames, it, clazz)
                    value?.let {
                        resultList.add(value)
                    }
                }
                return@executionResultList resultList
            }
            return@executionResultList ArrayList<T>()
        }
    }

    suspend fun <T> query(sql: String, tuple: List<Any>, clazz: Class<T>): List<T> {
        return this.query(sql, tuple, clazz, null)
    }

    suspend fun <T> query(sql: String, clazz: Class<T>, transaction: SQLConnection?): List<T> {
        return this.query(sql, ArrayList(), clazz, transaction)
    }

    suspend fun <T> query(sql: String, clazz: Class<T>): List<T> {
        return this.query(sql, ArrayList(), clazz, null)
    }


    suspend fun insert(sql: String, tuple: ArrayList<Any>, transaction: SQLConnection?): Boolean {
        val executeTuple = JsonArray()
        tuple.forEach {
            executeTuple.add(it)
        }
        log.debug("insert $sql")
        return execution(transaction) { connection ->
            val result = awaitResult<UpdateResult> {
                if (tuple.isNotEmpty()) {
                    connection.updateWithParams(sql, executeTuple, it)
                } else {
                    connection.update(sql, it)
                }
            }
            if (result.updated > 0) {
                return@execution true
            }
            return@execution false
        } ?: false
    }

    suspend fun insert(sql: String, tuple: ArrayList<Any>): Boolean {
        return insert(sql, tuple, null)
    }

    suspend fun insert(sql: String, transaction: SQLConnection?): Boolean {
        return insert(sql, ArrayList(), transaction)
    }

    suspend fun insert(sql: String): Boolean {
        return insert(sql, ArrayList())
    }


    suspend fun insertLastInsert(sql: String, tuple: ArrayList<Any>, transaction: SQLConnection?): Long {
        val executeTuple = JsonArray()
        tuple.forEach {
            executeTuple.add(it)
        }
        log.debug("insertLastIn: $sql")
        return execution(transaction) { connection ->
            val result = awaitResult<UpdateResult> {
                if (tuple.isNotEmpty()) {
                    connection.updateWithParams(sql, executeTuple, it)
                } else {
                    connection.update(sql, it)
                }
            }
            if (result.updated > 0) {
                return@execution result.updated.toLong()
            }
            return@execution -1L
        } as Long
    }

    suspend fun insertLastInsert(sql: String, tuple: ArrayList<Any>): Long {
        return insertLastInsert(sql, tuple, null)
    }

    suspend fun insertLastInsert(sql: String, transaction: SQLConnection?): Long {
        return insertLastInsert(sql, ArrayList(), transaction)
    }

    suspend fun insertLastInsert(sql: String): Long {
        return insertLastInsert(sql, ArrayList())
    }


    suspend fun update(sql: String, tuple: List<Any>, transaction: SQLConnection?): Boolean {
        val executeTuple = JsonArray()
        tuple.forEach {
            executeTuple.add(it)
        }
        log.debug("update : $sql")
        return execution(transaction) { connection ->
            val result = awaitResult<UpdateResult> {
                if (tuple.isNotEmpty()) {
                    connection.updateWithParams(sql, executeTuple, it)
                } else {
                    connection.update(sql, it)
                }
            }
            if (result.updated > 0) {
                return@execution true
            }
            return@execution false
        } ?: false
    }

    suspend fun update(sql: String, tuple: List<Any>): Boolean {
        return update(sql, tuple)
    }

    suspend fun update(sql: String): Boolean {
        return update(sql, ArrayList())
    }

    suspend fun update(sql: String, transaction: SQLConnection?): Boolean {
        return update(sql, ArrayList(), transaction)
    }


    suspend fun delete(sql: String, tuple: List<Any>, transaction: SQLConnection?): Boolean {
        val executeTuple = JsonArray()
        tuple.forEach {
            executeTuple.add(it)
        }
        log.debug("delete : $sql")
        return execution(transaction) { connection ->
            val result = awaitResult<UpdateResult> {
                if (tuple.isNotEmpty()) {
                    connection.updateWithParams(sql, executeTuple, it)
                } else {
                    connection.update(sql, it)
                }
            }
            if (result.updated > 0) {
                return@execution true
            }
            return@execution false
        } ?: false

    }

    suspend fun delete(sql: String, tuple: List<Any>): Boolean {
        return delete(sql, tuple, null)
    }

    suspend fun delete(sql: String): Boolean {
        return delete(sql, ArrayList())
    }

    suspend fun delete(sql: String, transaction: SQLConnection?): Boolean {
        return delete(sql, ArrayList(), transaction)
    }

}