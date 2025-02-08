package com.geotab.mobile.sdk.logging

import android.util.Log
import com.geotab.mobile.sdk.module.app.AppLogEventListener

enum class LogLevel(val type: Int) {
    INFO(0),
    WARN(1),
    ERROR(2);

    override fun toString(): String {
        return when (type) {
            0 -> "INFO"
            1 -> "WARN"
            2 -> "ERROR"
            else -> "INFO"
        }
    }
}

class InternalAppLogging(
    private val listener: AppLogEventListener
) : Logging {

    companion object {
        @Volatile
        var appLogger: Logging? = null

        fun setListener(listener: AppLogEventListener) {
            synchronized(this) {
                val instance = InternalAppLogging(listener)
                appLogger = instance
            }
        }
    }

    override fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun info(tag: String, message: String) {
        Log.i(tag, message)
        listener.triggerLogEvents(LogLevel.INFO, tag, message)
    }

    override fun warn(tag: String, message: String) {
        Log.w(tag, message)
        listener.triggerLogEvents(LogLevel.WARN, tag, message)
    }

    override fun error(tag: String, message: String) {
        Log.e(tag, message)
        listener.triggerLogEvents(LogLevel.ERROR, tag, message)
    }

    override fun error(tag: String, message: String, exception: Throwable) {
        Log.e(tag, message, exception)
        listener.triggerLogEvents(LogLevel.ERROR, tag, message, exception)
    }
}
