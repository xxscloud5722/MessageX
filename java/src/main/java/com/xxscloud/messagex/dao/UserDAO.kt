package com.xxscloud.messagex.dao

import com.google.inject.Inject
import com.xxscloud.messagex.core.xxs.MySQLCore
import com.xxscloud.messagex.data.UserDO
import io.vertx.ext.sql.SQLConnection

class UserDAO @Inject constructor(private val sqlCore: MySQLCore) {
    suspend fun insert(user: UserDO, transaction: SQLConnection? = null): Boolean {
        return sqlCore.insert(
            """
                INSERT INTO `m_user`(`channel`, `open_id`, `nick_name`, `token`) 
                VALUES (?, ?, ?, ?);
            """.trimIndent(),
            arrayListOf(user.channel, "${user.channel}_${user.openId}", user.nickName, user.token),
            transaction
        )
    }

    suspend fun exist(channel: String, openId: String, transaction: SQLConnection? = null): Boolean {
        return sqlCore.queryResult(
            """
                SELECT COUNT(*) > 0 FROM m_user WHERE open_id = ?
            """.trimIndent(), arrayListOf("${channel}_$openId"), Boolean::class.java, transaction
        ) ?: false
    }

    suspend fun getByOpenId(id: String, transaction: SQLConnection? = null): UserDO? {
        return sqlCore.queryFirst(
            """
                SELECT * FROM m_user WHERE open_id = ?
            """.trimIndent(), arrayListOf(id), UserDO::class.java, transaction
        )
    }

    suspend fun getById(id: String, transaction: SQLConnection? = null): UserDO? {
        return sqlCore.queryFirst(
            """
                SELECT * FROM m_user WHERE id = ?
            """.trimIndent(), arrayListOf(id), UserDO::class.java, transaction
        )
    }

}