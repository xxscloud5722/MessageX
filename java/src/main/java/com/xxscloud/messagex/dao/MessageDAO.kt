package com.xxscloud.messagex.dao

import com.google.inject.Inject
import com.xxscloud.messagex.core.xxs.MySQLCore
import com.xxscloud.messagex.core.xxs.SQL
import com.xxscloud.messagex.data.MessageDO
import com.xxscloud.messagex.data.MessageDTO
import io.vertx.ext.sql.SQLConnection


class MessageDAO @Inject constructor(private val sqlCore: MySQLCore) {
    suspend fun insert(messageDO: MessageDO, transaction: SQLConnection? = null): Boolean {
        val sql = SQL(
            """
                 INSERT INTO `m_message`(`sender`, `title`, `abstract`, `cover`, `content`) 
                    VALUES (#{sender}, #{title}, #{abstract}, #{cover}, #{content});
            """
        )
        sql.add(messageDO)
        val id = sqlCore.insertLastInsert(
            sql,
            transaction
        )
        messageDO.id = id.toString()
        return true
    }

    suspend fun getById(id: String, transaction: SQLConnection? = null): MessageDO? {
        val sql = SQL(
            """
                  SELECT * FROM m_message WHERE id = #{id}
            """
        )
        sql.add("id", id)
        return sqlCore.queryFirst(
            sql, MessageDO::class.java, transaction
        )
    }

    suspend fun getAbstractById(id: String, transaction: SQLConnection? = null): MessageDO? {
        val sql = SQL(
            """
                  SELECT id, title, abstract, cover FROM m_message WHERE id = #{id}
            """
        )
        sql.add("id", id)
        return sqlCore.queryFirst(
            sql, MessageDO::class.java, transaction
        )
    }

    suspend fun getMessageList(id: String, messageId: String, status: Int, transaction: SQLConnection? = null): List<MessageDTO> {
        val sql = SQL(
            """
                SELECT mm.id, mm.title, mm.abstract, mm.cover, mm.create_time FROM m_message mm
                LEFT JOIN m_message_queue mmq ON mmq.message_id = mm.id
            """
        )
        sql.where { it ->
            it.trim {
                it.join("AND mmq.status = #{status}", status >= 0)
                it.join("AND mmq.user_id = #{id}", messageId.isNotEmpty())
                it.join("AND mm.id < #{messageId}", messageId.isNotEmpty() && messageId.toInt() > 0)
            }
        }
        sql.join(
            """ 
                ORDER BY mm.id DESC
                LIMIT 100
        """
        )
        sql.add("id", id)
        sql.add("status", status)
        sql.add("messageId", messageId)
        return sqlCore.query(
            sql, MessageDTO::class.java, transaction
        )
    }


    fun getUnreadMessageCountByUserId(id: String): Int {
        return 1
    }
}