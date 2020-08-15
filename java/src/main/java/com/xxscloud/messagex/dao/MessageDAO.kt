package com.xxscloud.messagex.dao

import com.google.inject.Inject
import com.xxscloud.messagex.core.MySQLCore
import com.xxscloud.messagex.data.MessageDO
import com.xxscloud.messagex.data.MessageDTO
import io.vertx.ext.sql.SQLConnection


class MessageDAO @Inject constructor(private val sqlCore: MySQLCore) {
    suspend fun insert(messageDO: MessageDO, transaction: SQLConnection? = null): Boolean {
        val id = sqlCore.insertLastInsert(
            """
                INSERT INTO `m_message`( `title`, `abstract`, `cover`, `content`) 
                VALUES (?, ?, ?, ?);
            """.trimIndent(),
            arrayListOf(messageDO.title, messageDO.abstract, messageDO.cover, messageDO.content),
            transaction
        )
        messageDO.id = id.toString()
        return true
    }

    suspend fun getById(id: String, transaction: SQLConnection? = null): MessageDO? {
        return sqlCore.queryFirst(
            """
                SELECT * FROM m_message WHERE id = ?
            """.trimIndent(), arrayListOf(id), MessageDO::class.java, transaction
        )
    }

    suspend fun getMessageList(id: String, messageId: String, transaction: SQLConnection? = null): List<MessageDTO> {
        val parameter = arrayListOf(id)
        var sql = """
                SELECT mm.id, mm.title, mm.abstract, mm.cover, mm.create_time FROM m_message mm
                LEFT JOIN m_message_queue mmq ON mmq.message_id = mm.id
                WHERE mmq.user_id = ? AND mmq.status = 0
            """
        if (messageId.isNotEmpty() && messageId.toInt() > 0) {
            sql += " AND mm.id < ?"
            parameter.add(messageId)
        }
        sql += """ 
                ORDER BY mm.id DESC
                LIMIT 100
        """
        return sqlCore.query(
            sql, parameter, MessageDTO::class.java, transaction
        )
    }


    fun getUnreadMessageCountByUserId(id: String): Int {
        return 1
    }
}