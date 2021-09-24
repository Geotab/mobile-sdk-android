package com.geotab.mobile.sdk.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object JsonUtil {
    fun <T> toJson(src: T): String {
        return Gson().toJson(src)
    }
    inline fun <reified T> fromJson(json: String): T {
        return Gson().fromJson(json, object : TypeToken<T>() {}.type)
    }
}
