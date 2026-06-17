package com.geotab.mobile.sdk.logging.sentry

import android.content.Context
import android.content.pm.PackageInfo
import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Sentry environment types
 */
enum class SentryEnvironment(val value: String) {
    ALPHA("alpha"),
    DEVELOPMENT("development"),
    PRODUCTION("production")
}

/**
 * Configuration for SentryLogger initialization.
 *
 * Sample rates control the per-level probability of forwarding non-exception logs
 * to the Sentry Explore > Logs tab. They do not affect the Issues tab: logs with
 * exceptions, structured events (isSentryEvent=true), and crashes always flow
 * through unsampled via the captureEvent path.
 *
 * Sample rates have no defaults, so callers must supply explicit values. The compiler
 * catches any new construction site that would otherwise blow the Sentry Logs quota.
 */
data class SentryConfig(
    val dsn: String,
    val environment: SentryEnvironment,
    val packageInfo: PackageInfo,
    val warnLogsSampleRate: Double, // Fraction of WARN logs forwarded to Sentry Logs tab
    val errorLogsSampleRate: Double, // Fraction of ERROR logs forwarded to Sentry Logs tab
    val debug: Boolean = false,
    val captureLevel: SentryLevel = SentryLevel.WARNING, // Minimum level to capture; INFO and below dropped at source
    val sendDebugLogs: Boolean = false // Whether to send DEBUG logs as breadcrumbs
) {
    init {
        require(warnLogsSampleRate in 0.0..1.0) {
            "warnLogsSampleRate must be in [0.0, 1.0], got $warnLogsSampleRate"
        }
        require(errorLogsSampleRate in 0.0..1.0) {
            "errorLogsSampleRate must be in [0.0, 1.0], got $errorLogsSampleRate"
        }
    }
}

/**
 * Listener that integrates with Sentry for crash reporting and error tracking.
 * Listens to log broadcasts and sends them to Sentry based on configured level.
 *
 * Use [create] factory function to instantiate, not the constructor directly.
 * The factory function ensures Sentry initialization happens on a background thread to prevent ANR.
 */

@Suppress("UnstableApiUsage")
@Keep
class SentryLogListener private constructor(
    private val captureLevel: SentryLevel,
    private val sendDebugLogs: Boolean,
    private val warnLogsSampleRate: Double,
    private val errorLogsSampleRate: Double
) : LogListener {

    companion object {
        private const val TAG = "SentryLogListener"

        @VisibleForTesting
        var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

        // Injected for testing; controls per-log random sampling decision
        @VisibleForTesting
        internal var random: () -> Double = { kotlin.random.Random.nextDouble() }

        @VisibleForTesting
        internal fun createForTest(
            captureLevel: SentryLevel = SentryLevel.WARNING,
            sendDebugLogs: Boolean = false,
            warnLogsSampleRate: Double = 0.01,
            errorLogsSampleRate: Double = 0.01
        ): SentryLogListener {
            require(warnLogsSampleRate in 0.0..1.0) { "warnLogsSampleRate must be in [0.0, 1.0], got $warnLogsSampleRate" }
            require(errorLogsSampleRate in 0.0..1.0) { "errorLogsSampleRate must be in [0.0, 1.0], got $errorLogsSampleRate" }
            return SentryLogListener(
                captureLevel = captureLevel,
                sendDebugLogs = sendDebugLogs,
                warnLogsSampleRate = warnLogsSampleRate,
                errorLogsSampleRate = errorLogsSampleRate
            )
        }

        /**
         * Creates a SentryLogListener instance with Sentry initialization on background thread.
         * This prevents ANR when SentryAndroid.init() blocks on HandlerThread.getLooper().
         *
         * @param config Sentry configuration including DSN, environment, and package info
         * @param context Application context
         * @param dispatcher Coroutine dispatcher for background work (defaults to ioDispatcher, injectable for testing)
         * @return Initialized SentryLogListener instance
         * @throws kotlinx.coroutines.TimeoutCancellationException if initialization takes longer than 30 seconds
         */
        suspend fun create(
            config: SentryConfig,
            context: Context,
            dispatcher: CoroutineDispatcher = ioDispatcher
        ): SentryLogListener = withTimeout(30_000L) {
            withContext(dispatcher) {
                // SentryAndroid.init() internally calls HandlerThread.getLooper() which blocks
                // Move to background thread to prevent ANR on main thread
                if (!Sentry.isEnabled()) {
                    Log.d(TAG, "Initializing Sentry on background thread (${Thread.currentThread().name})...")
                    val startTime = System.currentTimeMillis()

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

                    val duration = System.currentTimeMillis() - startTime
                    Log.d(TAG, "SentryAndroid.init() completed in ${duration}ms on thread ${Thread.currentThread().name}")
                } else {
                    Log.d(TAG, "Sentry already initialized, skipping init")
                }

                // Return the constructed instance
                SentryLogListener(
                    captureLevel = config.captureLevel,
                    sendDebugLogs = config.sendDebugLogs,
                    warnLogsSampleRate = config.warnLogsSampleRate,
                    errorLogsSampleRate = config.errorLogsSampleRate
                )
            }
        }

        private fun buildReleaseName(packageInfo: PackageInfo): String {
            val version = packageInfo.versionName ?: "unknown_version"
            val build = packageInfo.versionCode.toString()
            val packageName = packageInfo.packageName ?: "unknown_package"
            return "$packageName ${version}_$build"
        }
    }

    override fun onLogEvent(event: LogEvent) {
        val sentryLevel = event.level.toSentryLevel()

        // Breadcrumbs attach to Issues (no Logs quota impact). Captured outside
        // the captureLevel gate so INFO/WARN breadcrumbs still provide context on
        // future exceptions. Sentry events manage their own scope, skip breadcrumb.
        if (!event.isSentryEvent && (sentryLevel != SentryLevel.DEBUG || sendDebugLogs)) {
            addBreadcrumb(sentryLevel, event.tag, event.message)
        }

        // Logs/Events dispatch gated by captureLevel. Callers of Logger.shared.event()
        // must use WARN or higher in prod (captureLevel=WARNING); INFO/DEBUG events
        // are dropped here.
        if (sentryLevel.ordinal >= captureLevel.ordinal) {
            if (event.isSentryEvent) {
                sendSentryEvent(sentryLevel, event.tag, event.message, event.exception, event.tags, event.context)
            } else {
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
     *
     * The Logs tab path applies per-level sampling (warnLogsSampleRate / errorLogsSampleRate)
     * to stay within Sentry's Logs ingestion quota. The Issues tab path is never sampled.
     */
    private fun sendSentryLog(level: SentryLevel, tag: String, message: String, exception: Throwable? = null) {
        val formattedMessage = "[$tag] $message"

        if (exception == null) {
            // No exception: send to Sentry Explore > Logs (apply per-level sampling)
            val sampleRate = when (level) {
                SentryLevel.WARNING -> warnLogsSampleRate
                SentryLevel.ERROR -> errorLogsSampleRate
                SentryLevel.FATAL -> 1.0
                else -> return
            }
            if (random() > sampleRate) return // Sampled out, drop this log entry

            when (level) {
                SentryLevel.WARNING -> Sentry.logger().warn(formattedMessage)
                SentryLevel.ERROR -> Sentry.logger().error(formattedMessage)
                SentryLevel.FATAL -> Sentry.logger().fatal(formattedMessage)
                else -> Unit
            }
        } else {
            // Has exception: send as Event to Issues tab (NEVER sampled)
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
