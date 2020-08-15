package com.xxscloud.messagex.exception

class ParameterException : RuntimeException {
    constructor(message: ExceptionMessageEnum) : super(message.code + "\n" + message.message)

    constructor(message: String) : super(message)
}