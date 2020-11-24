package com.xxscloud.messagex.dao

import com.google.inject.Inject
import com.xxscloud.messagex.core.xxs.MySQLCore
import com.xxscloud.messagex.core.xxs.SQL
import com.xxscloud.messagex.data.UserDO
import com.xxscloud.messagex.data.UserGroupDO
import io.vertx.ext.sql.SQLConnection


class UserGroupDAO @Inject constructor(private val sqlCore: MySQLCore) {
    suspend fun insert(userGroup: UserGroupDO, transaction: SQLConnection? = null): Boolean {
        val sql = SQL(
            """
                INSERT INTO `m_user_group`(`channel_id`, `name`)VALUES (#{channelId}, #{name})
            """
        )
        sql.add(userGroup)
        val id = sqlCore.insertLastInsert(sql, transaction)
        userGroup.id = id.toString()
        return true
    }

    suspend fun getById(id: String, transaction: SQLConnection? = null): UserGroupDO? {
        val sql = SQL(
            """
                 SELECT * FROM m_user_group WHERE id = #{id}
            """
        )
        sql.add("id", id)
        return sqlCore.queryFirst(
            sql, UserGroupDO::class.java, transaction
        )
    }

    suspend fun getUserList(groupId: String, transaction: SQLConnection? = null): List<UserDO> {
        val sql = SQL(
            """
                SELECT mu.id, mu.open_id, mu.nick_name, mu.status FROM m_user_group_relationship mugr
                LEFT JOIN m_user mu ON mu.id = mugr.user_id
                WHERE mugr.group_id = #{groupId}
            """
        )
        sql.add("groupId", groupId)
        return sqlCore.query(
            sql, UserDO::class.java, transaction
        )
    }

    suspend fun getGroupAllList(channelId: String, transaction: SQLConnection? = null): List<UserGroupDO> {
        val sql = SQL(
            """
            SELECT * FROM m_user_group WHERE channel_id = #{channelId}
            """
        )
        sql.add("channelId", channelId)
        return sqlCore.query(
            sql, UserGroupDO::class.java, transaction
        )
    }

    suspend fun joinGroup(groupId: String, userId: String, transaction: SQLConnection? = null): Boolean {
        val sql = SQL(
            """
             INSERT INTO `m_user_group_relationship`(`user_id`, `group_id`) VALUES (#{groupId}, #{userId});
            """
        )
        sql.add("groupId", groupId)
        sql.add("userId", userId)
        return sqlCore.insert(sql, transaction)
    }


    suspend fun exist(groupId: String, userId: String, transaction: SQLConnection? = null): Boolean {
        val sql = SQL(
            """
             SELECT COUNT(*) > 0 FROM `m_user_group_relationship` WHERE user_id = #{userId} AND group_id = #{groupId};
            """
        )
        sql.add("groupId", groupId)
        sql.add("userId", userId)
        return sqlCore.queryResult(sql, Boolean::class.java, transaction) ?: false
    }
}