package com.xxscloud.messagex.core.xxs

import com.alibaba.druid.pool.DruidDataSource
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.spi.DataSourceProvider
import java.util.*
import javax.sql.DataSource


class MySQLDataSourceProvider : DataSourceProvider {

    companion object {
        private lateinit var druid: DruidDataSource

        init {
            System.setProperty("druid.mysql.usePingMethod", "false")
        }

        fun init(config: JsonObject?) {
            val value = Properties()
            value["druid.name"] = "mysql"
            value["druid.url"] = config?.getString("url")
            value["druid.username"] = config?.getString("user")
            value["druid.password"] = config?.getString("password")
            value["druid.driverClassName"] = "com.mysql.cj.jdbc.Driver"

            config?.getString("initialSize")?.let {
                value["druid.initialSize"] = it
            }

            config?.getString("maxActive")?.let {
                value["druid.maxActive"] = it
            }

            config?.getString("minIdle")?.let {
                value["druid.minIdle"] = it
            }

            config?.getString("maxWait")?.let {
                value["druid.maxWait"] = it
            }

            value["druid.validationQuery"] = "SELECT 1;"

            config?.getString("testOnBorrow")?.let {
                value["druid.testOnBorrow"] = it
            }

            config?.getString("testOnReturn")?.let {
                value["druid.testOnReturn"] = it
            }
            config?.getString("testWhileIdle")?.let {
                value["druid.testWhileIdle"] = it
            }

            config?.getString("timeBetweenEvictionRunsMillis")?.let {
                value["druid.timeBetweenEvictionRunsMillis"] = it
            }

            config?.getString("minEvictableIdleTimeMillis")?.let {
                value["druid.minEvictableIdleTimeMillis"] = it
            }

            config?.getString("connectionInitSqls")?.let {
                value["druid.connectionInitSqls"] = it
            }

            value["druid.filters"] = "stat,wall"
            config?.getString("filters")?.let {
                value["druid.filters"] = it
            }

            config?.getString("proxyFilters")?.let {
                value["druid.proxyFilters"] = it
            }


            val druid = DruidDataSource()
            druid.configFromPropety(value)
            Companion.druid = druid
        }
    }

    override fun maximumPoolSize(dataSource: DataSource?, config: JsonObject?): Int {
        if (dataSource is DruidDataSource) {
            config?.let {
                return it.getInteger("maxActive") ?: 15
            }
        }
        return -1
    }

    override fun getDataSource(config: JsonObject?): DataSource {
        return druid
    }

    override fun close(dataSource: DataSource?) {
        if (dataSource is DruidDataSource) {
            dataSource.close()
        }
    }
}