package com.geotab.mobile.sdk.logging

import androidx.annotation.Keep

/**
 * Log level enum
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
    val exception: Throwable? = null,
    val tags: Map<String, String>? = null,
    val context: Map<String, Any>? = null,
    val isSentryEvent: Boolean = false
)

/**
 * Interface for log listeners
 */
@Keep
interface LogListener {
    fun onLogEvent(event: LogEvent)
}

/**
 * Broadcaster that distributes logs to multiple listeners.
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

    /**
     * Creates Sentry logs/breadcrumbs.
     * Use for informational logging that doesn't require alerts.
     */
    override fun log(
        level: BroadcastLogLevel,
        tag: String,
        message: String
    ) {
        broadcast(LogEvent(level, tag, message, isSentryEvent = false))
    }

    /**
     * Creates Sentry events.
     * Use for errors/warnings that require investigation.
     */
    override fun event(
        level: BroadcastLogLevel,
        tag: String,
        message: String,
        exception: Throwable?,
        tags: Map<String, String>,
        context: Map<String, Any>
    ) {
        broadcast(LogEvent(level, tag, message, exception, tags, context, isSentryEvent = true))
    }
}
