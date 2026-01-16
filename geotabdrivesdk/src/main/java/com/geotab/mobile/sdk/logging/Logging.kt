package com.geotab.mobile.sdk.logging

import androidx.annotation.Keep

@Keep
interface Logging {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warn(tag: String, message: String)
    fun error(tag: String, message: String)
    fun error(tag: String, message: String, exception: Throwable)

    /**
     * Creates breadcrumbs/logs in Sentry (sent to Sentry Explore > Logs).
     * Use for informational logging that doesn't require alerts.
     * Note: Geotab's Sentry plan has limited log storage.
     */
    fun log(
        level: BroadcastLogLevel,
        tag: String,
        message: String
    )

    /**
     * Creates Sentry events (sent to Issues tab, generates alerts).
     * Use for errors/warnings that require investigation.
     *
     * @param tags Key-value pairs for Sentry filtering/grouping
     * @param context Additional context data for debugging
     */
    fun event(
        level: BroadcastLogLevel,
        tag: String,
        message: String,
        exception: Throwable? = null,
        tags: Map<String, String> = emptyMap(),
        context: Map<String, Any> = emptyMap()
    )
}

@Keep
object Logger {
    var shared: Logging = LogBroadcaster().apply {
        // Add Android Log listener by default
        addListener(AndroidLogListener())
    }
}
