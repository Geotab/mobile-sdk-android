package com.geotab.mobile.sdk.logging.sentry

import android.content.Context
import android.content.pm.PackageInfo
import androidx.annotation.Keep
import com.geotab.mobile.sdk.logging.BroadcastLogLevel
import com.geotab.mobile.sdk.logging.LogEvent
import com.geotab.mobile.sdk.logging.LogListener
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.Message

/**
 * Sentry environment types
 */
enum class SentryEnvironment(val value: String) {
    ALPHA("alpha"),
    DEVELOPMENT("development"),
    PRODUCTION("production")
}

/**
 * Configuration for SentryLogger initialization
 */
data class SentryConfig(
    val dsn: String,
    val environment: SentryEnvironment,
    val packageInfo: PackageInfo,
    val debug: Boolean = false,
    val captureLevel: SentryLevel = SentryLevel.INFO, // Minimum level to capture as events
    val sendDebugLogs: Boolean = false // Whether to send DEBUG logs as breadcrumbs
)

/**
 * Listener that integrates with Sentry for crash reporting and error tracking.
 * Listens to log broadcasts and sends them to Sentry based on configured level.
 */

@Suppress("UnstableApiUsage")
@Keep
class SentryLogListener(
    config: SentryConfig,
    context: Context
) : LogListener {

    private val captureLevel: SentryLevel = config.captureLevel
    private val sendDebugLogs: Boolean = config.sendDebugLogs

    init {
        if (!Sentry.isEnabled()) {
            SentryAndroid.init(context) { options: SentryOptions ->
                options.dsn = config.dsn
                options.isDebug = config.debug
                options.environment = config.environment.value
                options.release = buildReleaseName(config.packageInfo)
                options.tracesSampleRate = if (config.environment == SentryEnvironment.ALPHA ||
                    config.environment == SentryEnvironment.DEVELOPMENT
                ) 1.0 else 0.0
                options.isSendDefaultPii = true
                options.isEnableAutoSessionTracking = true
                options.isAttachStacktrace = true
                options.logs.isEnabled = true
            }
        }
    }

    override fun onLogEvent(event: LogEvent) {
        val sentryLevel = event.level.toSentryLevel()

        // Send to Sentry if level is high enough
        if (sentryLevel.ordinal >= captureLevel.ordinal) {
            if (event.isSentryEvent) {
                // Explicit Sentry event - always creates event
                sendSentryEvent(sentryLevel, event.tag, event.message, event.exception, event.tags, event.context)
            } else {
                // Simple log - only add breadcrumbs (unless it's DEBUG OR sendDebugLogs is false)
                if (sentryLevel != SentryLevel.DEBUG || sendDebugLogs) {
                    addBreadcrumb(sentryLevel, event.tag, event.message)
                }

                sendSentryLog(sentryLevel, event.tag, event.message, event.exception)
            }
        }
    }

    private fun addBreadcrumb(level: SentryLevel, tag: String, message: String) {
        Sentry.addBreadcrumb(
            Breadcrumb().apply {
                this.level = level
                category = tag
                this.message = message
            }
        )
    }

    /**
     * Sends a log to Sentry. Logs without exceptions are sent to the Logs tab,
     * while logs with exceptions are sent as events to the Issues tab.
     */
    private fun sendSentryLog(level: SentryLevel, tag: String, message: String, exception: Throwable? = null) {
        val formattedMessage = "[$tag] $message"

        if (exception == null) {
            // No exception: send to Sentry Explore > Logs
            when (level) {
                SentryLevel.DEBUG -> {
                    // Only send DEBUG logs if debugging is enabled
                    if (sendDebugLogs) {
                        Sentry.logger().debug(formattedMessage)
                    }
                }
                SentryLevel.INFO -> Sentry.logger().info(formattedMessage)
                SentryLevel.WARNING -> Sentry.logger().warn(formattedMessage)
                SentryLevel.ERROR -> Sentry.logger().error(formattedMessage)
                SentryLevel.FATAL -> Sentry.logger().fatal(formattedMessage)
            }
        } else {
            // Has exception: send as Event to Issues tab
            val event = SentryEvent(exception)
            event.level = level
            event.message = Message().apply { formatted = formattedMessage }
            Sentry.captureEvent(event)
        }
    }

    /**
     * Creates and send Sentry event object (sent to Issues tab, generates alerts).
     * Always creates an event regardless of whether exception is present.
     */
    private fun sendSentryEvent(
        level: SentryLevel,
        tag: String,
        message: String,
        exception: Throwable?,
        tags: Map<String, String>?,
        context: Map<String, Any>?
    ) {
        val formattedMessage = "[$tag] $message"

        // Always create a Sentry event
        val event = if (exception != null) {
            SentryEvent(exception)
        } else {
            SentryEvent()
        }

        event.level = level
        event.message = Message().apply { formatted = formattedMessage }

        Sentry.captureEvent(event) { scope ->
            // Set the level on scope
            scope.level = level

            // Add tags to scope
            tags?.forEach { (key, value) ->
                scope.setTag(key, value)
            }

            // Add context/extras to scope
            context?.forEach { (key, value) ->
                scope.setExtra(key, value.toString())
            }
        }
    }

    private fun buildReleaseName(packageInfo: PackageInfo): String {
        val version = packageInfo.versionName ?: "unknown_version"
        val build = packageInfo.versionCode.toString()
        val packageName = packageInfo.packageName ?: "unknown_package"
        return "$packageName ${version}_$build"
    }
}

/**
 * Convert BroadcastLogLevel to SentryLevel
 */
private fun BroadcastLogLevel.toSentryLevel(): SentryLevel {
    return when (this) {
        BroadcastLogLevel.DEBUG -> SentryLevel.DEBUG
        BroadcastLogLevel.INFO -> SentryLevel.INFO
        BroadcastLogLevel.WARN -> SentryLevel.WARNING
        BroadcastLogLevel.ERROR -> SentryLevel.ERROR
    }
}
