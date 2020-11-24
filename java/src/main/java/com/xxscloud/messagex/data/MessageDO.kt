package com.xxscloud.messagex.data

import java.util.*

data class MessageDO(
    var id: String = "",
    var sender: String = "",
    var title: String = "",
    var abstract: String = "",
    var cover: String = "",
    var content: String = "",
    var createTime: Date? = null
)