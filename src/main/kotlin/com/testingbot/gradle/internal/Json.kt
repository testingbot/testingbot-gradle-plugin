package com.testingbot.gradle.internal

import com.google.gson.Gson

/** Thin wrapper around a shared Gson instance. */
internal object Json {
    private val gson = Gson()

    fun <T> fromJson(body: String, type: Class<T>): T = gson.fromJson(body, type)

    fun toJson(value: Any?): String = gson.toJson(value)
}
