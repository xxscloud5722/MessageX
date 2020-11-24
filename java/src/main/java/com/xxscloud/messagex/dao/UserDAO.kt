package com.xxscloud.messagex.dao

import com.google.inject.Inject
import com.xxscloud.messagex.core.xxs.MySQLCore
import com.xxscloud.messagex.core.xxs.SQL
import com.xxscloud.messagex.data.UserDO
import io.vertx.ext.sql.SQLConnection

class UserDAO @Inject constructor(private val sqlCore: MySQLCore) {
    suspend fun insert(user: UserDO, transaction: SQLConnection? = null): Boolean {
        val sql = SQL(
            """
                INSERT INTO `m_user`(`channel`, `open_id`, `nick_name`, `token`) 
                VALUES (#{channel}, #{openId}, #{nickName}, CONCAT('API','_',UUID()));
            """
        )
        sql.add(user)
        return sqlCore.insert(
            sql,
            transaction
        )
    }

    suspend fun exist(openId: String, transaction: SQLConnection? = null): Boolean {
        val sql = SQL(
            """
                SELECT COUNT(*) > 0 FROM m_user WHERE open_id = #{openId}
            """
        )
        sql.add("openId", openId)
        return sqlCore.queryResult(sql, Boolean::class.java, transaction) ?: false
    }

    suspend fun getByOpenId(id: String, transaction: SQLConnection? = null): UserDO? {
        val sql = SQL(
            """
                SELECT * FROM m_user WHERE open_id = #{id}
            """
        )
        sql.add("id", id)
        return sqlCore.queryFirst(
            sql, UserDO::class.java, transaction
        )
    }

    suspend fun getById(id: String, transaction: SQLConnection? = null): UserDO? {
        val sql = SQL(
            """
             SELECT * FROM m_user WHERE id = #{id}
            """
        )
        sql.add("id", id)
        return sqlCore.queryFirst(
            sql, UserDO::class.java, transaction
        )
    }

    suspend fun getByToken(token: String, transaction: SQLConnection? = null): UserDO? {
        val sql = SQL(
            """
                SELECT * FROM m_user WHERE token = #{token}
            """
        )
        sql.add("token", token)
        return sqlCore.queryFirst(
            sql, UserDO::class.java, transaction
        )
    }

    suspend fun updateToken(id: String, token: String, transaction: SQLConnection? = null): Boolean {
        val sql = SQL(
            """
                UPDATE m_user SET token = #{token} WHERE id = #{id}
            """
        )
        sql.add("id", id)
        sql.add("token", token)
        return sqlCore.update(sql, transaction)
    }

}