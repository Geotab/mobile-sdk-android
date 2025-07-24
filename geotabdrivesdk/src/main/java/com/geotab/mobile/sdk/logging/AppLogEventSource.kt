package com.geotab.mobile.sdk.logging

import androidx.annotation.Keep
import com.geotab.mobile.sdk.models.ModuleEvent
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.util.JsonUtil
import java.util.Date

@Keep
data class LogDetail(
    val message: String,
    val level: Int
)

interface AppLogEventListener {
    fun triggerLogEvents(type: LogLevel, tag: String, message: String, error: Throwable? = null)
}

class AppLogEventSource(
    private val push: (ModuleEvent, ((Result<Success<String>, Failure>) -> Unit)) -> Unit,
    private val logLimit: Int = 10, // default is no more than 10 logs fired per 30 mins
    private val timeInterval: Int = 30 * 60
) : AppLogEventListener {
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

    override fun triggerLogEvents(type: LogLevel, tag: String, message: String, error: Throwable?) {
        var detailMessage = "[$tag] [$type] $message"
        error?.message?.let { msg ->
            detailMessage = msg
        }
        // check for loglimit
        if (!throttle()) {
            return
        }
        push(ModuleEvent("app.log", "{detail: ${logJson(detailMessage, type.type)} }")) {}
    }

    private fun logJson(detailMessage: String, type: Int): String {
        val logMessage = LogDetail(detailMessage, type)
        return JsonUtil.toJson(logMessage)
    }
}
