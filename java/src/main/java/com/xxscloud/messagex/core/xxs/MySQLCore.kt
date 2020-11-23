package com.xxscloud.messagex.core.xxs


import com.xxscloud.messagex.config.Config
import com.xxscloud.messagex.exception.CoreException
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.ResultSet
import io.vertx.ext.sql.SQLConnection
import io.vertx.ext.sql.UpdateResult
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.ext.sql.commitAwait
import io.vertx.kotlin.ext.sql.getConnectionAwait
import io.vertx.kotlin.ext.sql.rollbackAwait
import io.vertx.kotlin.ext.sql.setAutoCommitAwait
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.text.SimpleDateFormat
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

            statistics = Config.getValue("mysql.statistics", "0").toInt() > 0

            val host = Config.getValue("mysql.host", "127.0.0.1")
            val port = Config.getValue("mysql.port", "3306")
            val database = Config.getValue("mysql.database")
            val user = Config.getValue("mysql.user")
            val password = Config.getValue("mysql.password")


            MySQLDataSourceProvider.init(
                    JsonObject()
                            .put(
                                    "url",
                                    "jdbc:mysql://${host}:${port}/${database}?useUnicode=true&characterEncoding=UTF-8&" +
                                            "allowMultiQueries=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
                            )
                            .put("user", user)
                            .put("password", password)
                            .put("maxActive", "500")
                            .put("minIdle", "5")
                            .put("initialSize", "3")
            )
            val client = JDBCClient.createShared(
                    vertx,
                    JsonObject().put("provider_class", "com.xxscloud.messagex.core.xxs.MySQLDataSourceProvider")
            )


            log.info("MySQL Loading complete Host: $host Port: $port Database: $database")
            Companion.client = client
            sqlCore = MySQLCore()
            Companion.vertx = vertx
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
                    if (System.currentTimeMillis() - startTime > 3000) {
                        log.warn("[MySQL] execution transaction total time: ${System.currentTimeMillis() - startTime} ms")
                    } else {
                        log.debug("[MySQL] execution transaction total time: ${System.currentTimeMillis() - startTime} ms")
                    }
                }
            }
        }

        fun isNotEmpty(v: Any?): Boolean {
            if (v == null) {
                return false
            }
            return v.toString().isNotEmpty()
        }
    }


    suspend fun getConnection(): SQLConnection {
        return client.getConnectionAwait()
    }

    private fun setValue(it: Any): Any {
        return when (it) {
            is BigDecimal -> it.toDouble()
            is BigInteger -> it.toLong()
            is Date -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(it)
            else -> it
        }
    }

    @Suppress("DuplicatedCode")
    private suspend fun <T> execution(sqlConnection: SQLConnection?, fn: suspend (SQLConnection) -> T?): T? {
        val connection = sqlConnection ?: awaitResult { client.getConnection(it) }
        try {
            return fn(connection)
        } finally {
            if (sqlConnection == null) {
                connection.close()
            }
        }
    }

    @Suppress("DuplicatedCode")
    private suspend fun <T> executionResultList(
            sqlConnection: SQLConnection?,
            fn: suspend (SQLConnection) -> List<T>
    ): List<T> {
        val connection = sqlConnection ?: awaitResult { client.getConnection(it) }
        try {
            return fn(connection)
        } finally {
            if (sqlConnection == null) {
                connection.close()
            }
        }
    }

    suspend fun <T> queryResult(multiple: List<Array<Any>>, clazz: Class<T>, transaction: SQLConnection? = null): T? {
        return execution(transaction) { connection ->
            var result = JsonArray()
            multiple.forEach { item ->
                val sqlStartTime = System.currentTimeMillis()
                result = awaitResult {
                    //创建参数
                    val sql = item[0].toString()
                    val tuple = (item[1] as ArrayList<*>)
                    val executeTuple = JsonArray()
                    tuple.forEach { p ->
                        executeTuple.add(setValue(p))
                    }
                    if (tuple.isNotEmpty()) {
                        connection.querySingleWithParams(sql, executeTuple, it)
                    } else {
                        connection.querySingle(sql, it)
                    }
                } ?: JsonArray()
                log.debug("queryResult execution_${System.currentTimeMillis() - sqlStartTime}ms ${item[0]}")
            }
            if (result.size() > 0) {
                return@execution BeanUtils.getValue(clazz, result, 0)
            }
            return@execution null
        }
    }

    suspend fun <T> queryResult(sql: String, tuple: List<Any>, clazz: Class<T>, transaction: SQLConnection? = null): T? {
        val executeTuple = JsonArray()
        tuple.forEach {
            executeTuple.add(setValue(it))
        }
        return execution(transaction) { connection ->
            val sqlStartTime = System.currentTimeMillis()
            val result = awaitResult<JsonArray?> {
                if (tuple.isNotEmpty()) {
                    connection.querySingleWithParams(sql, executeTuple, it)
                } else {
                    connection.querySingle(sql, it)
                }
            }
            log.debug("queryResult: execution_${System.currentTimeMillis() - sqlStartTime}ms $sql")
            if (result != null && result.size() > 0) {
                return@execution BeanUtils.getValue(clazz, result, 0)
            }
            return@execution null
        }
    }

    suspend fun <T> queryResult(sqlHandel: SQL, clazz: Class<T>, transaction: SQLConnection? = null): T? {
        return if (sqlHandel.isMultiple()) {
            val sqlResult = sqlHandel.parsingMultiple()
            queryResult(sqlResult, clazz, transaction)
        } else {
            val sqlResult = sqlHandel.parsing()
            val sql = sqlResult[0].toString()
            val tuple = (sqlResult[1] as ArrayList<*>)
            queryResult(sql, tuple, clazz, transaction)
        }
    }

    suspend fun <T> queryFirst(multiple: List<Array<Any>>, clazz: Class<T>, transaction: SQLConnection? = null): T? {
        return execution(transaction) { connection ->
            var result = ResultSet()
            multiple.forEach { item ->
                val sqlStartTime = System.currentTimeMillis()
                result = awaitResult {
                    //创建参数
                    val sql = item[0].toString()
                    val tuple = (item[1] as ArrayList<*>)
                    val executeTuple = JsonArray()
                    tuple.forEach { p ->
                        executeTuple.add(setValue(p))
                    }

                    //执行SQL
                    if (tuple.isNotEmpty()) {
                        connection.queryWithParams(sql, executeTuple, it)
                    } else {
                        connection.query(sql, it)
                    }
                }
                log.debug("queryFirst: execution_${System.currentTimeMillis() - sqlStartTime}ms ${item[0]}")
            }
            if (result.results.size > 0) {
                return@execution BeanUtils.toBean(result.columnNames, result.results[0], clazz)
            }
            return@execution null
        }
    }

    suspend fun <T> queryFirst(sql: String, tuple: List<Any>, clazz: Class<T>, transaction: SQLConnection? = null): T? {
        val executeTuple = JsonArray()
        tuple.forEach {
            executeTuple.add(setValue(it))
        }
        return execution(transaction) { connection ->
            val sqlStartTime = System.currentTimeMillis()
            val result = awaitResult<ResultSet?> {
                if (tuple.isNotEmpty()) {
                    connection.queryWithParams(sql, executeTuple, it)
                } else {
                    connection.query(sql, it)
                }
            }
            log.debug("queryFirst: execution_${System.currentTimeMillis() - sqlStartTime}ms $sql")
            if (result != null && result.results.size > 0) {
                return@execution BeanUtils.toBean(result.columnNames, result.results[0], clazz)
            }
            return@execution null
        }
    }

    suspend fun <T> queryFirst(sqlHandel: SQL, clazz: Class<T>, transaction: SQLConnection? = null): T? {
        return if (sqlHandel.isMultiple()) {
            val sqlResult = sqlHandel.parsingMultiple()
            queryFirst(sqlResult, clazz, transaction)
        } else {
            val sqlResult = sqlHandel.parsing()
            val sql = sqlResult[0].toString()
            val tuple = (sqlResult[1] as ArrayList<*>)
            queryFirst(sql, tuple, clazz, transaction)
        }
    }


    suspend fun <T> query(multiple: List<Array<Any>>, clazz: Class<T>, transaction: SQLConnection? = null): ArrayList<T> {
        return execution(transaction) { connection ->
            var result = ResultSet()
            multiple.forEach { item ->
                val sqlStartTime = System.currentTimeMillis()
                result = awaitResult {
                    //创建参数
                    val sql = item[0].toString()
                    val tuple = (item[1] as ArrayList<*>)
                    val executeTuple = JsonArray()
                    tuple.forEach { p ->
                        executeTuple.add(setValue(p))
                    }

                    //执行SQL
                    if (tuple.isNotEmpty()) {
                        connection.queryWithParams(sql, executeTuple, it)
                    } else {
                        connection.query(sql, it)
                    }
                }
                log.debug("query: execution_${System.currentTimeMillis() - sqlStartTime}ms ${item[0]}")
            }
            if (result.results.size > 0) {
                val resultList = ArrayList<T>()
                result.results.forEach {
                    val value = BeanUtils.toBean(result.columnNames, it, clazz)
                    value?.let {
                        resultList.add(value)
                    }
                }
                return@execution resultList
            }
            return@execution ArrayList<T>()
        } ?: ArrayList<T>()
    }

    suspend fun <T> query(sql: String, tuple: List<Any>, clazz: Class<T>, transaction: SQLConnection? = null): List<T> {
        val executeTuple = JsonArray()
        tuple.forEach {
            executeTuple.add(setValue(it))
        }
        return executionResultList(transaction) { connection ->
            val sqlStartTime = System.currentTimeMillis()
            val result = awaitResult<ResultSet?> {
                if (tuple.isNotEmpty()) {
                    connection.queryWithParams(sql, executeTuple, it)
                } else {
                    connection.query(sql, it)
                }
            }
            log.debug("query: execution_${System.currentTimeMillis() - sqlStartTime}ms $sql")
            if (result != null && result.results.size > 0) {
                val resultList = ArrayList<T>()
                result.results.forEach {
                    val value = BeanUtils.toBean(result.columnNames, it, clazz)
                    value?.let {
                        resultList.add(value)
                    }
                }
                return@executionResultList resultList
            }
            return@executionResultList ArrayList<T>()
        }
    }

    suspend fun <T> query(sqlHandel: SQL, clazz: Class<T>, transaction: SQLConnection? = null): List<T> {
        return if (sqlHandel.isMultiple()) {
            val sqlResult = sqlHandel.parsingMultiple()
            query(sqlResult, clazz, transaction)
        } else {
            val sqlResult = sqlHandel.parsing()
            val sql = sqlResult[0].toString()
            val tuple = (sqlResult[1] as ArrayList<*>)
            query(sql, tuple, clazz, transaction)
        }
    }

    suspend fun insert(sql: String, tuple: List<Any>, transaction: SQLConnection? = null): Boolean {
        val executeTuple = JsonArray()
        tuple.forEach {
            executeTuple.add(setValue(it))
        }
        return execution(transaction) { connection ->
            val sqlStartTime = System.currentTimeMillis()
            val result = awaitResult<UpdateResult?> {
                if (tuple.isNotEmpty()) {
                    connection.updateWithParams(sql, executeTuple, it)
                } else {
                    connection.update(sql, it)
                }
            }
            log.debug("insert: execution_${System.currentTimeMillis() - sqlStartTime}ms $sql")
            if (result != null && result.updated > 0) {
                return@execution true
            }
            return@execution false
        } ?: false
    }

    suspend fun insert(sqlHandel: SQL, transaction: SQLConnection? = null): Boolean {
        val sqlResult = sqlHandel.parsing()
        val sql = sqlResult[0].toString()
        val tuple = (sqlResult[1] as ArrayList<*>)
        return insert(sql, tuple, transaction)
    }


    suspend fun insertLastInsert(multiple: List<Array<Any>>, transaction: SQLConnection? = null): Long {
        return execution(transaction) { connection ->
            var result = UpdateResult()
            multiple.forEach { item ->
                val sqlStartTime = System.currentTimeMillis()
                result = awaitResult {
                    //创建参数
                    val sql = item[0].toString()
                    val tuple = (item[1] as ArrayList<*>)
                    val executeTuple = JsonArray()
                    tuple.forEach { p ->
                        executeTuple.add(setValue(p))
                    }

                    //执行SQL
                    if (tuple.isNotEmpty()) {
                        connection.updateWithParams(sql, executeTuple, it)
                    } else {
                        connection.update(sql, it)
                    }
                }
                log.debug("insertLastInsert: execution_${System.currentTimeMillis() - sqlStartTime}ms ${item[0]}")
            }
            if (result.updated > 0) {
                return@execution (result.keys.list[0] as BigInteger).toLong()
            }
            return@execution -1
        } ?: -1
    }

    suspend fun insertLastInsert(sql: String, tuple: List<Any>, transaction: SQLConnection? = null): Long {
        val executeTuple = JsonArray()
        tuple.forEach {
            executeTuple.add(setValue(it))
        }
        return execution(transaction) { connection ->
            val sqlStartTime = System.currentTimeMillis()
            val result = awaitResult<UpdateResult?> {
                if (tuple.isNotEmpty()) {
                    connection.updateWithParams(sql, executeTuple, it)
                } else {
                    connection.update(sql, it)
                }
            }
            log.debug("insertLastInsert: execution_${System.currentTimeMillis() - sqlStartTime}ms $sql")
            if (result != null && result.updated > 0) {
                return@execution (result.keys.list[0] as BigInteger).toLong()
            }
            return@execution -1L
        } as Long
    }

    suspend fun insertLastInsert(sqlHandel: SQL, transaction: SQLConnection? = null): Long {
        return if (sqlHandel.isMultiple()) {
            val sqlResult = sqlHandel.parsingMultiple()
            insertLastInsert(sqlResult, transaction)
        } else {
            val sqlResult = sqlHandel.parsing()
            val sql = sqlResult[0].toString()
            val tuple = (sqlResult[1] as ArrayList<*>)
            insertLastInsert(sql, tuple, transaction)
        }
    }

    suspend fun insertLastInserts(sql: String, tuple: List<Any>, transaction: SQLConnection? = null): List<Long>? {
        val executeTuple = JsonArray()
        tuple.forEach {
            executeTuple.add(setValue(it))
        }
        return execution(transaction) { connection ->
            val sqlStartTime = System.currentTimeMillis()
            val result = awaitResult<UpdateResult?> {
                if (tuple.isNotEmpty()) {
                    connection.updateWithParams(sql, executeTuple, it)
                } else {
                    connection.update(sql, it)
                }
            }
            log.debug("insertLastInserts: execution_${System.currentTimeMillis() - sqlStartTime}ms $sql")
            if (result != null && result.updated > 0) {
                val resultList = ArrayList<Long>()
                result.keys.list.forEach {
                    resultList.add(it.toString().toLong())
                }
                return@execution resultList
            }
            return@execution null
        }
    }

    suspend fun insertLastInserts(sqlHandel: SQL, transaction: SQLConnection? = null): List<Long>? {
        val sqlResult = sqlHandel.parsing()
        val sql = sqlResult[0].toString()
        val tuple = (sqlResult[1] as ArrayList<*>)
        return insertLastInserts(sql, tuple, transaction)
    }

    suspend fun update(sql: String, tuple: List<Any>, transaction: SQLConnection? = null): Boolean {
        val executeTuple = JsonArray()
        tuple.forEach {
            executeTuple.add(setValue(it))
        }
        return execution(transaction) { connection ->
            val sqlStartTime = System.currentTimeMillis()
            val result = awaitResult<UpdateResult?> {
                if (tuple.isNotEmpty()) {
                    connection.updateWithParams(sql, executeTuple, it)
                } else {
                    connection.update(sql, it)
                }
            }
            log.debug("update: execution_${System.currentTimeMillis() - sqlStartTime}ms $sql")
            if (result != null && result.updated > 0) {
                return@execution true
            }
            return@execution false
        } ?: false
    }

    suspend fun update(sqlHandel: SQL, transaction: SQLConnection? = null): Boolean {
        val sqlResult = sqlHandel.parsing()
        val sql = sqlResult[0].toString()
        val tuple = (sqlResult[1] as ArrayList<*>)
        return update(sql, tuple, transaction)
    }

    suspend fun delete(sql: String, tuple: List<Any>, transaction: SQLConnection? = null): Boolean {
        val executeTuple = JsonArray()
        tuple.forEach {
            executeTuple.add(setValue(it))
        }
        return execution(transaction) { connection ->
            val sqlStartTime = System.currentTimeMillis()
            val result = awaitResult<UpdateResult?> {
                if (tuple.isNotEmpty()) {
                    connection.updateWithParams(sql, executeTuple, it)
                } else {
                    connection.update(sql, it)
                }
            }
            log.debug("delete: execution_${System.currentTimeMillis() - sqlStartTime}ms $sql")
            if (result != null && result.updated > 0) {
                return@execution true
            }
            return@execution false
        } ?: false

    }

    suspend fun delete(sqlHandel: SQL, transaction: SQLConnection? = null): Boolean {
        val sqlResult = sqlHandel.parsing()
        val sql = sqlResult[0].toString()
        val tuple = (sqlResult[1] as ArrayList<*>)
        return delete(sql, tuple, transaction)
    }

    suspend fun <T> queryTable(sqlHandel: SQL, clazz: Class<T>, transaction: SQLConnection? = null): Table<T> {
        if (sqlHandel.isMultiple()) {
            //获取总条数
            val multipleList = sqlHandel.parsingMultiple()
            val item = multipleList[multipleList.size - 1]
            val sql = item[0].toString()
            val tuple = (item[1] as ArrayList<*>)
            val totalCount = queryTableCount(sql, tuple, transaction)
            if (totalCount <= 0) {
                return Table(totalCount = totalCount, rows = ArrayList())
            }

            //查询总数据
            val table = sqlHandel.getTable()
            if (table != null) {
                item[0] = "$sql LIMIT ${table.limit} , ${table.pageSize}"
            }

            val dataList = query(multipleList, clazz, transaction)
            return Table(totalCount = totalCount, rows = dataList)
        } else {
            //查询总条数
            val sqlResult = sqlHandel.parsing()
            val sql = sqlResult[0].toString()
            val tuple = (sqlResult[1] as ArrayList<*>)
            val totalCount = queryTableCount(sql, tuple, transaction)
            if (totalCount <= 0) {
                return Table(totalCount = totalCount, rows = ArrayList())
            }
            //查询总数据
            val table = sqlHandel.getTable()
            val dataList = if (table != null) {
                query("$sql LIMIT ${table.limit} , ${table.pageSize}", tuple, clazz, transaction)
            } else {
                query(sql, tuple, clazz, transaction)
            }

            return Table(totalCount = totalCount, rows = dataList)
        }
    }

    private suspend fun queryTableCount(sql: String, tuple: List<Any>, transaction: SQLConnection? = null): Long {
        val sqlIndex = getIndex(sql)
        if (sqlIndex <= 0) {
            throw CoreException("SQL error")
        }

        //查询数据
        val queryTotalSql = "SELECT COUNT(*) " + sql.substring(sqlIndex)
        return queryResult(queryTotalSql, tuple, Long::class.java, transaction) ?: 0
    }

    private fun getIndex(sql: String): Int {
        var index = 0
        val word = ArrayList<Char>()
        sql.toUpperCase().toCharArray().forEachIndexed { i, it ->
            word.add(0, it)
            if (word.size > 4) {
                word.removeAt(4)
            }
            if (it == '(') {
                index++
            }
            if (it == ')') {
                index--
            }
            if (index == 0 && word.size > 3 && word[3] == 'F' && word[2] == 'R' && word[1] == 'O' && word[0] == 'M') {
                return i - 3
            }
        }
        return 0
    }
}

