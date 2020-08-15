package com.xxscloud.messagex.dao

import com.google.inject.Inject
import com.xxscloud.messagex.core.MySQLCore
import com.xxscloud.messagex.data.UserDO
import com.xxscloud.messagex.data.UserGroupDO
import io.vertx.ext.sql.SQLConnection


class UserGroupDAO @Inject constructor(private val sqlCore: MySQLCore) {
    suspend fun insert(userGroup: UserGroupDO, transaction: SQLConnection? = null): Boolean {
        val id = sqlCore.insertLastInsert(
            """
                INSERT INTO `m_user_group`(`name`) 
                VALUES (?);
            """.trimIndent(), arrayListOf(userGroup.name), transaction
        )
        userGroup.id = id.toString()
        return true
    }

    suspend fun getById(id: String, transaction: SQLConnection? = null): UserGroupDO? {
        return sqlCore.queryFirst(
            """
                SELECT * FROM m_user_group WHERE id = ?
            """.trimIndent(), arrayListOf(id), UserGroupDO::class.java, transaction
        )
    }

    suspend fun getUserList(id: String, transaction: SQLConnection? = null): List<UserDO> {
        return sqlCore.query(
            """
                SELECT mu.* FROM m_user_group_relationship mugr
                LEFT JOIN m_user mu ON mu.id = mugr.user_id
                WHERE mugr.group_id = ?
            """.trimIndent(), arrayListOf(id), UserDO::class.java, transaction
        )
    }

    suspend fun getGroupAllList(transaction: SQLConnection? = null): List<UserGroupDO> {
        return sqlCore.query(
            """
                SELECT * FROM m_user_group
            """.trimIndent(), UserGroupDO::class.java, transaction
        )
    }

    suspend fun joinGroup(groupId: String, userId: String, transaction: SQLConnection? = null): Boolean {
        return sqlCore.insert(
            """
                INSERT INTO `m_user_group_relationship`(`user_id`, `group_id`) 
                VALUES (?, ?);
            """.trimIndent(), arrayListOf(userId, groupId), transaction
        )
    }

    suspend fun exist(groupId: String, userId: String, transaction: SQLConnection? = null): Boolean {
        return sqlCore.queryResult(
            """
                SELECT COUNT(*) > 0 FROM `m_user_group_relationship` WHERE user_id = ? AND group_id = ?
            """.trimIndent(), arrayListOf(userId, groupId), Boolean::class.java, transaction
        ) ?: false
    }
}