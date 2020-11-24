package com.xxscloud.messagex.dao

import com.google.inject.Inject
import com.xxscloud.messagex.core.xxs.MySQLCore
import com.xxscloud.messagex.core.xxs.SQL
import com.xxscloud.messagex.data.UserDO
import io.vertx.ext.sql.SQLConnection


class MessageQueueDAO @Inject constructor(private val sqlCore: MySQLCore) {
    suspend fun insert(id: String, recipient: String, transaction: SQLConnection? = null): Boolean {
        val sql = SQL(
            """
                INSERT INTO `m_message_queue`(`user_id`, `message_id`) 
                VALUES (recipient, id);
            """
        )
        sql.add("id", id)
        sql.add("recipient", recipient)
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
}