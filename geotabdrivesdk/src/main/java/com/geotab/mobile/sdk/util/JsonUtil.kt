package com.geotab.mobile.sdk.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonWriter
import java.io.OutputStream
import java.io.OutputStreamWriter

object JsonUtil {
    fun <T> toJson(src: T): String {
        return Gson().toJson(src)
    }
    inline fun <reified T> fromJson(json: String): T {
        return Gson().fromJson(json, object : TypeToken<T>() {}.type)
    }
    inline fun <reified T> toJsonStreaming(out: OutputStream?, src: T) {
        val writer = JsonWriter(OutputStreamWriter(out, "UTF-8"))
        Gson().toJson(src, T::class.java, writer)
        writer.close()
    }
}
