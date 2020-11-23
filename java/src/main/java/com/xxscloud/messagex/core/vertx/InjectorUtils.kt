package com.xxscloud.messagex.core.vertx

import com.google.inject.Injector

object InjectorUtils {
    private lateinit var injector: Injector
    fun get(): Injector {
        return injector
    }

    fun factory(fn: () -> Injector): Injector {
        injector = fn()
        return injector
    }

    fun <T> getBean(clazz: Class<T>): T {
        return get().getInstance(clazz)
    }
}