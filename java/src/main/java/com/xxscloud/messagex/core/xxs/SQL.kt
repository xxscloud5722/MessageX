package com.xxscloud.messagex.core.xxs

import io.vertx.core.json.JsonObject
import java.util.regex.Pattern

class SQL(private var sql: String = "") {
    private val parameter = HashMap<String, Any>()
    private var table: Table<*>? = null

    companion object {
        private val P = Pattern.compile("#\\{[^}]*}")
    }

    fun getTable(): Table<*>? {
        return table
    }

    fun where(fn: (sql: SQL) -> SQL): SQL {
        val sql = fn(SQL()).toString().trim()
        if (sql.isNotEmpty()) {
            this.sql = "${this.sql} WHERE $sql"
        }
        return this
    }

    fun set(fn: (sql: SQL) -> SQL): SQL {
        var sql = fn(SQL()).toString().trim()
        if (sql.isNotEmpty()) {
            if (sql.endsWith(",")) {
                sql = sql.substring(0, sql.length - 1)
            }
            this.sql = "${this.sql} SET $sql "
        }
        return this
    }

    fun trim(fn: (sql: SQL) -> SQL): SQL {
        var sql = fn(SQL()).toString().trim()
        arrayListOf("AND ", "OR ").forEach {
            val index = sql.toUpperCase().indexOf(it)
            if (index == 0) {
                sql = sql.substring(4)
            }
        }
        this.sql = " $sql"
        return this
    }

    fun join(sql: String): SQL {
        this.sql += " $sql "
        return this
    }

    fun join(sql: String, condition: Boolean?): SQL {
        condition?.let {
            if (condition) {
                this.sql += " $sql "
            }
        }
        return this
    }

    fun join(sql: String, condition: () -> Boolean): SQL {
        if (condition()) {
            return this.join(" $sql")
        }
        return this
    }

    fun join(sql1: String, sql2: String, condition: Boolean?): SQL {
        if (condition != null && condition) {
            this.sql += " $sql1 "
        } else {
            this.sql += " $sql2 "
        }
        return this
    }

    fun add(vararg args: Any) {
        this.parameter.putAll(add(args.toList()))
    }

    fun add(key: String, value: Any) {
        this.parameter[key] = value
    }


    fun parsing(): Array<Any> {
        return parsing(this.sql)
    }

    fun parsingMultiple(): List<Array<Any>> {
        val multiple = ArrayList<Array<Any>>()
        this.sql.split(";").forEach {
            if (it.isEmpty()) {
                return@forEach
            }
            val result = parsing(it)
            if (result[0].toString().trim().isNotEmpty()) {
                multiple.add(result)
            }
        }
        return multiple
    }


    fun foreach(sql: String, separator: String, rows: List<*>) {
        val forSql = StringBuilder()
        rows.forEachIndexed { i, it ->
            if (it == null) {
                return@forEachIndexed
            }
            val sqlBuilder = StringBuilder(sql)
            if (i != rows.size - 1) {
                sqlBuilder.append(separator)
            }
            val m = P.matcher(sql)
            //替换SQL
            while (m.find()) {
                for (index in 0..m.groupCount()) {
                    val field = m.group(index)
                    val fieldName = field.substring(2, field.length - 1)
                    while (true) {
                        val rowIndex = sqlBuilder.indexOf(field)
                        if (rowIndex <= -1) {
                            break
                        }
                        sqlBuilder.replace(rowIndex, rowIndex + field.length, "#{i$i.$fieldName}")
                    }
                }
            }
            forSql.append(sqlBuilder)
            //写入参数
            addPrefix("i$i.", it)
        }
        this.sql += forSql
    }

    fun isMultiple(): Boolean {
        return sql.split(";").filter { it.trim().isNotEmpty() }.size > 1
    }


    private fun parsing(text: String): Array<Any> {
        val m = P.matcher(text)
        val args = ArrayList<Any>()
        val sql = StringBuilder(text)
        while (m.find()) {
            for (i in 0..m.groupCount()) {
                val field = StringBuilder(m.group(i))
                while (true) {
                    val fieldIndex = sql.indexOf(field.toString())
                    if (fieldIndex <= -1) {
                        break
                    }
                    sql.replace(fieldIndex, fieldIndex + field.length, "?")
                }
                val fieldName = field.substring(2, field.length - 1)
                if (this.parameter[fieldName] != null) {
                    args.add(this.parameter[fieldName] ?: "")
                } else {
                    throw RuntimeException("$fieldName error")
                }
            }
        }
        return arrayOf(sql, args)
    }

    private fun add(args: List<Any?>): HashMap<String, Any> {
        val result = HashMap<String, Any>()
        args.forEach { value ->
            if (value == null) {
                return@forEach
            }
            when (value) {
                is JsonObject -> {
                    value.map.forEach {
                        if (it.value != null) {
                            result[it.key] = it.value
                        }
                    }
                }
                is Map<*, *> -> {
                    value.forEach {
                        if (it.value != null) {
                            result[it.key.toString()] = it.value ?: ""
                        }
                    }
                }
                is Table<*> -> {
                    this.table = value
                }
                else -> {
                    value.javaClass.methods.forEach { method ->
                        if (method.name.startsWith("get")) {
                            val r = method.invoke(value)
                            var key = method.name.substring(3)
                            key = key.substring(0, 1).toLowerCase() + key.substring(1)
                            r?.let {
                                result[key] = it
                            }
                        } else if (method.name.startsWith("is")) {
                            val r = method.invoke(value)
                            val key = method.name
                            r?.let {
                                result[key] = it
                            }
                        }
                    }
                }
            }
        }
        return result
    }

    private fun addPrefix(prefix: String, it: Any) {
        val result = add(arrayListOf(it))
        val map = HashMap<String, Any>()
        result.forEach {
            map[prefix + it.key] = it.value
        }
        this.parameter.putAll(map)
    }

    override fun toString(): String {
        return sql
    }
}