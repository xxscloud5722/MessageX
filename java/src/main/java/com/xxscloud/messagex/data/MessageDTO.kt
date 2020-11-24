package com.xxscloud.messagex.data

import java.util.*
import kotlin.collections.ArrayList

data class MessageDTO(
    var id: String = "",
    var sender: String = "",
    var title: String = "",
    var recipient: List<String>? = null,
    var recipientGroup: List<String>? = null,
    var content: String = "",
    var abstract: String = "",
    var cover: String = "",
    var createTime: Date? = null,
    var status: Int = 0

)