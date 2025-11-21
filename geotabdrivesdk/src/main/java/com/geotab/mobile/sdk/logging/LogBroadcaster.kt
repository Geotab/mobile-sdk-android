package com.geotab.mobile.sdk.logging

import androidx.annotation.Keep

/**
 * Log level enum matching iOS implementation
 */
@Keep
enum class BroadcastLogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

/**
 * Log event that gets broadcast to all listeners
 */
@Keep
data class LogEvent(
    val level: BroadcastLogLevel,
    val tag: String,
    val message: String,
    val exception: Throwable? = null
)

/**
 * Interface for log listeners (similar to iOS NotificationCenter pattern)
 */
@Keep
interface LogListener {
    fun onLogEvent(event: LogEvent)
}

/**
 * Broadcaster that distributes logs to multiple listeners.
 * Similar to iOS's NotificationCenter pattern where DefaultLogger posts to NotificationCenter
 * and multiple listeners (AppLogEventSource, SentryLogger) receive the events.
 */
@Keep
class LogBroadcaster : Logging {
    private val listeners = mutableListOf<LogListener>()

    /**
     * Add a listener that will receive all log events
     */
    fun addListener(listener: LogListener) {
        synchronized(listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }
    }

    /**
     * Remove a listener
     */
    fun removeListener(listener: LogListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    private fun broadcast(event: LogEvent) {
        synchronized(listeners) {
            listeners.forEach { listener ->
                try {
                    listener.onLogEvent(event)
                } catch (e: Exception) {
                    // Prevent one listener from breaking others
                    // Use System.err to avoid android.util.Log dependency in unit tests
                    System.err.println("LogBroadcaster: Listener failed: ${e.message}")
                }
            }
        }
    }

    override fun debug(tag: String, message: String) {
        broadcast(LogEvent(BroadcastLogLevel.DEBUG, tag, message))
    }

    override fun info(tag: String, message: String) {
        broadcast(LogEvent(BroadcastLogLevel.INFO, tag, message))
    }

    override fun warn(tag: String, message: String) {
        broadcast(LogEvent(BroadcastLogLevel.WARN, tag, message))
    }

    override fun error(tag: String, message: String) {
        broadcast(LogEvent(BroadcastLogLevel.ERROR, tag, message))
    }

    override fun error(tag: String, message: String, exception: Throwable) {
        broadcast(LogEvent(BroadcastLogLevel.ERROR, tag, message, exception))
    }
}
