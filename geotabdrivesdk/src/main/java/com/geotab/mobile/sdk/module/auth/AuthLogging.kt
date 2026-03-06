package com.geotab.mobile.sdk.module.auth

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.geotab.mobile.sdk.logging.BroadcastLogLevel
import com.geotab.mobile.sdk.logging.Logging

/**
 * Auth module logging constants and extensions.
 */
object Auth {
    const val LOGGING_TAG = "Auth"

    object TagKey {
        const val EVENT_CATEGORY = "event_category"
        const val AUTH_RESULT = "auth_result"
        const val AUTH_FLOW = "auth_flow"
        const val APP_STATE = "app_state"
    }

    object TagValue {
        const val AUTH_ATTEMPT = "auth_attempt"
        const val FAILURE = "failure"
    }

    enum class FlowType(val value: String) {
        LOGIN("login"),
        REAUTH("reauth"),
        TOKEN_REFRESH("token_refresh"),
        BACKGROUND_REFRESH("background_refresh"),
        BACKGROUND_REFRESH_RETRY("background_refresh_retry"),
        LOGOUT("logout")
    }

    object AppState {
        const val FOREGROUND = "foreground"
        const val BACKGROUND = "background"
    }

    enum class ContextKey(val value: String) {
        USERNAME("username"),
        AVAILABLE_BYTES("available_bytes"),
        TOTAL_BYTES("total_bytes"),
        OS_STATUS("os_status"),
        RECOVERABLE("recoverable"),
        REQUIRES_REAUTH("requires_reauth"),
        RETRY_ATTEMPT("retry_attempt"),
        STAGE("stage"),
        REASON("reason")
    }

    object LogoutStage {
        const val TOKEN_REVOCATION = "token_revocation"
        const val SECURE_STORAGE_DELETION = "secure_storage_deletion"
        const val END_SESSION = "end_session"
    }
}

/**
 * Extension function for Logging to log auth failures with structured context.
 *
 * @param context Application context for determining app state
 * @param username The username associated with the auth attempt
 * @param flowType The type of auth flow (login, reauth, token_refresh, etc.)
 * @param error The error that occurred
 * @param additionalContext Optional additional context to include
 */
fun Logging.authFailure(
    context: Context,
    username: String,
    flowType: Auth.FlowType,
    error: Throwable?,
    additionalContext: Map<Auth.ContextKey, Any>? = null
) {
    val appState = getAppState(context)

    val tags = mapOf(
        Auth.TagKey.EVENT_CATEGORY to Auth.TagValue.AUTH_ATTEMPT,
        Auth.TagKey.AUTH_RESULT to Auth.TagValue.FAILURE,
        Auth.TagKey.AUTH_FLOW to flowType.value,
        Auth.TagKey.APP_STATE to appState
    )

    val contextData = mutableMapOf<String, Any>(
        Auth.ContextKey.USERNAME.value to username
    )

    // Add storage information
    try {
        val stat = StatFs(Environment.getDataDirectory().path)
        contextData[Auth.ContextKey.AVAILABLE_BYTES.value] = stat.availableBytes
        contextData[Auth.ContextKey.TOTAL_BYTES.value] = stat.totalBytes
    } catch (e: Throwable) {
        // Ignore if we can't get storage info (handles both Exception and NoSuchMethodError in tests)
    }

    // Add additional context if provided
    additionalContext?.forEach { (key, value) ->
        contextData[key.value] = value
    }

    // Use the event() method with tags and context for structured Sentry logging
    this.event(
        level = BroadcastLogLevel.ERROR,
        tag = Auth.LOGGING_TAG,
        message = "Auth attempt failed",
        exception = error,
        tags = tags,
        context = contextData
    )
}

/**
 * Determines the current app state (foreground/background).
 */
private fun getAppState(context: Context): String {
    return try {
        val authUtil = AuthUtil.getInstance()
        if (authUtil.isAppInBackground(context)) {
            Auth.AppState.BACKGROUND
        } else {
            Auth.AppState.FOREGROUND
        }
    } catch (e: Throwable) {
        // Default to foreground if we can't determine state (handles test environments)
        Auth.AppState.FOREGROUND
    }
}
