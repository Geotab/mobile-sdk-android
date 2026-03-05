package com.geotab.mobile.sdk.logging

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
object Logger {
    var shared: Logging = LogBroadcaster().apply {
        // Add Android Log listener by default
        addListener(AndroidLogListener())
    }
}
