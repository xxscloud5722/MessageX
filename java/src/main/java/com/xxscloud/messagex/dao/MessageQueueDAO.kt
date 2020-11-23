package com.xxscloud.messagex.dao

import com.google.inject.Inject
import com.xxscloud.messagex.core.xxs.MySQLCore
import com.xxscloud.messagex.data.UserDO
import io.vertx.ext.sql.SQLConnection


class MessageQueueDAO @Inject constructor(private val sqlCore: MySQLCore) {
    suspend fun insert(id: String, recipient: String, transaction: SQLConnection? = null): Boolean {
        return sqlCore.insert(
            """
                INSERT INTO `m_message_queue`(`user_id`, `message_id`) 
                VALUES (?, ?);
            """.trimIndent(), arrayListOf(recipient, id), transaction
        )
    }

    suspend fun getByMessageId(messageId: String, transaction: SQLConnection? = null): List<UserDO> {
        return sqlCore.query(
            """
                SELECT mu.* FROM m_message_queue mmq
                LEFT JOIN m_user mu ON mu.id = mmq.user_id
                WHERE mmq.message_id = ?
            """.trimIndent(), arrayListOf(messageId), UserDO::class.java, transaction
        )
    }
}