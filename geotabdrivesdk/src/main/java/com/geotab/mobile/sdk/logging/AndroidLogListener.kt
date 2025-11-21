package com.geotab.mobile.sdk.logging

import android.util.Log
import androidx.annotation.Keep

/**
 * Listener that forwards log events to Android's built-in Log system.
 * This is equivalent to iOS's DefaultLogger sending to os.Logger.
 */
@Keep
class AndroidLogListener : LogListener {
    override fun onLogEvent(event: LogEvent) {
        when (event.level) {
            BroadcastLogLevel.DEBUG -> Log.d(event.tag, event.message, event.exception)
            BroadcastLogLevel.INFO -> Log.i(event.tag, event.message, event.exception)
            BroadcastLogLevel.WARN -> Log.w(event.tag, event.message, event.exception)
            BroadcastLogLevel.ERROR -> Log.e(event.tag, event.message, event.exception)
        }
    }
}
