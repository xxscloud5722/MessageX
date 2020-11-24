package com.xxscloud.messagex.data

data class UserGroupDTO(
    var id: String = "",
    var userId: String = "",
    var userList: List<UserDO>? = null,

    var users: List<String>? = null
)