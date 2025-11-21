package com.geotab.mobile.sdk.logging

import androidx.annotation.Keep
import com.geotab.mobile.sdk.models.ModuleEvent
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.util.JsonUtil
import java.util.Date

/**
 * Log detail for BigQuery events
 */
@Keep
private data class BigQueryLogDetail(
    val message: String,
    val level: Int
)

/**
 * Listener that sends logs to BigQuery via the scriptGateway.
 * This is equivalent to iOS's AppLogEventSource listening to NotificationCenter.
 *
 * Filters out DEBUG logs and throttles to prevent overwhelming BigQuery.
 */
@Keep
class BigQueryLogListener(
    private val push: (ModuleEvent, ((Result<Success<String>, Failure>) -> Unit)) -> Unit,
    private val logLimit: Int = 10, // max 10 logs per time interval
    private val timeInterval: Int = 30 * 60 // 30 minutes in seconds
) : LogListener {

    private var numberOfLogs: Int = 0
    private var lastResetDate: Date = Date()

    private fun reset() {
        numberOfLogs = 0
        lastResetDate = Date()
    }

    private fun throttle(): Boolean {
        val diffInSec = (Date().time - lastResetDate.time) / 1000
        if (diffInSec > timeInterval) {
            reset()
        }
        numberOfLogs++
        return (numberOfLogs <= logLimit)
    }

    override fun onLogEvent(event: LogEvent) {
        // Don't send DEBUG logs to BigQuery (matching iOS behavior)
        if (event.level == BroadcastLogLevel.DEBUG) {
            return
        }

        // Check throttle limit
        if (!throttle()) {
            return
        }

        var detailMessage = "[${event.tag}] [${event.level}] ${event.message}"
        event.exception?.message?.let { msg ->
            detailMessage += " $msg"
        }

        val logLevel = when (event.level) {
            BroadcastLogLevel.INFO -> 0
            BroadcastLogLevel.WARN -> 1
            BroadcastLogLevel.ERROR -> 2
            BroadcastLogLevel.DEBUG -> 0 // Should not reach here, but default to INFO
        }

        val logDetail = BigQueryLogDetail(detailMessage, logLevel)
        val logJson = JsonUtil.toJson(logDetail)

        push(ModuleEvent("app.log", "{detail: $logJson }")) {}
    }
}
