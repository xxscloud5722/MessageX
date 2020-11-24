package com.xxscloud.messagex.dao

import com.google.inject.Inject
import com.xxscloud.messagex.core.xxs.MySQLCore
import com.xxscloud.messagex.core.xxs.SQL
import com.xxscloud.messagex.data.UserDO
import io.vertx.ext.sql.SQLConnection


class MessageQueueDAO @Inject constructor(private val sqlCore: MySQLCore) {
    suspend fun insert(messageId: String, userId: String, transaction: SQLConnection? = null): Boolean {
        val sql = SQL(
            """
                INSERT INTO `m_message_queue`(`user_id`, `message_id`) 
                VALUES (#{userId}, #{messageId});
            """
        )
        sql.add("messageId", messageId)
        sql.add("userId", userId)
        return sqlCore.insert(
            sql, transaction
        )
    }

    suspend fun getByMessageId(messageId: String, transaction: SQLConnection? = null): List<UserDO> {
        val sql = SQL(
            """
                SELECT mu.* FROM m_message_queue mmq
                LEFT JOIN m_user mu ON mu.id = mmq.user_id
                WHERE mmq.message_id = #{messageId}
            """
        )
        sql.add("messageId", messageId)
        return sqlCore.query(
            sql, UserDO::class.java, transaction
        )
    }

    suspend fun markMessage(userId: String, messageId: String, transaction: SQLConnection? = null): Boolean {
        val sql = SQL(
            """
                UPDATE m_message_queue SET status = 1, read_time = NOW() 
                WHERE message_id = #{messageId} AND user_id = #{userId}
            """
        )
        sql.add("messageId", messageId)
        sql.add("userId", userId)
        return sqlCore.update(sql, transaction)
    }
}