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
    val sendDebugLogs: Boolean = false // Whether to send DEBUG logs as breadcrumbs (matches iOS debugging flag)
)

/**
 * Listener that integrates with Sentry for crash reporting and error tracking.
 * Listens to log broadcasts and sends them to Sentry based on configured level.
 *
 * Similar to iOS's SentryLogger which wraps the DefaultLogger and adds Sentry integration.
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

        // Add breadcrumbs - skip DEBUG logs unless sendDebugLogs is enabled (matches iOS behavior)
        if (sentryLevel != SentryLevel.DEBUG || sendDebugLogs) {
            addBreadcrumb(sentryLevel, event.tag, event.message)
        }

        // Send to Sentry if level is high enough
        if (sentryLevel.ordinal >= captureLevel.ordinal) {
            sendToSentry(sentryLevel, event.tag, event.message, event.exception)
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

    private fun sendToSentry(level: SentryLevel, tag: String, message: String, exception: Throwable? = null) {
        val formattedMessage = "[$tag] $message"

        if (exception == null) {
            // No exception: send to Sentry Explore > Logs (matches iOS log() method)
            when (level) {
                SentryLevel.DEBUG -> {
                    // Only send DEBUG logs if debugging is enabled (matches iOS)
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
            // Has exception: send as Event to Issues tab (matches iOS event() method)
            val event = SentryEvent(exception)
            event.level = level
            event.message = Message().apply { formatted = formattedMessage }
            Sentry.captureEvent(event)
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
