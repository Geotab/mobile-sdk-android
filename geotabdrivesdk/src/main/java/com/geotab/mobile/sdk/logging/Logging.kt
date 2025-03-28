package com.geotab.mobile.sdk.logging

import android.util.Log
import androidx.annotation.Keep

@Keep
interface Logging {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warn(tag: String, message: String)
    fun error(tag: String, message: String)
    fun error(tag: String, message: String, exception: Throwable)
}

@Keep
class DefaultLogging : Logging {
    override fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun info(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun warn(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun error(tag: String, message: String) {
        Log.e(tag, message)
    }

    override fun error(tag: String, message: String, exception: Throwable) {
        Log.e(tag, message, exception)
    }
}

@Keep
object Logger {
    var shared: Logging = DefaultLogging()
}
