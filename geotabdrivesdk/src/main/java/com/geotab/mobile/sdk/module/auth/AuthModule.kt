package com.geotab.mobile.sdk.module.auth

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import com.geotab.mobile.sdk.logging.Logger
import com.geotab.mobile.sdk.module.Module
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.openid.appauth.AuthorizationService

class AuthModule(
    @Transient private val authUtil: AuthUtil
) : Module(MODULE_NAME) {
    private var isAuthServiceDisposed = false

    @Transient
    lateinit var context: Context

    internal val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val MODULE_NAME = "auth"
        const val TAG = "authModule"
        const val USERNAME_REQUIRED_ERROR_MESSAGE = "Username is required"
    }

    init {
        functions.add(LogoutFunction(module = this))
        functions.add(LoginFunction(module = this))
        functions.add(GetTokenFunction(module = this))
    }

    fun initValues(activity: ComponentActivity) {
        context = activity.applicationContext
        authUtil.authService = AuthorizationService(context)

        authUtil.logoutActivityResultLauncherFunction(
            activityForResult = activity
        )
    }

    /**
     * Perform logout for a user.
     *
     * @throws Exception on failure
     */
    suspend fun logout(username: String) {
        authUtil.logout(
            context = context,
            username = username
        )
    }

    /**
     * Perform login for a user.
     *
     * @return AuthToken on success
     * @throws Exception on failure
     */
    suspend fun login(
        clientId: String,
        discoveryUri: Uri,
        username: String,
        redirectScheme: Uri,
        ephemeralSession: Boolean
    ): AuthToken {
        if (isAuthServiceDisposed) {
            authUtil.authService = AuthorizationService(context)
        }

        try {
            return authUtil.login(
                context = context,
                clientId = clientId,
                discoveryUri = discoveryUri,
                username = username,
                redirectScheme = redirectScheme,
                ephemeralSession = ephemeralSession
            )
        } finally {
            isAuthServiceDisposed = true
        }
    }

    /**
     * Retrieves a valid access token for the specified user.
     *
     * If the token is expired, this method will attempt to refresh it.
     * In some scenarios (non-recoverable errors, app in foreground), this may
     * trigger a browser-based re-authentication flow.
     *
     * @param username The username to get the token for
     * @return AuthToken containing the access token
     * @throws AuthError.NoAccessTokenFoundError if no token exists for the user
     * @throws AuthError.TokenRefreshFailed if token refresh fails
     * @throws AuthError if re-authentication is required but fails
     */
    suspend fun handleAuthToken(username: String): AuthToken {
        val authToken = authUtil.getValidAccessToken(context, username)
        return authToken ?: throw AuthError.NoAccessTokenFoundError(username)
    }

    /**
     * Handles exceptions from auth function calls (LoginFunction, LogoutFunction, GetTokenFunction).
     * Used by Function classes to provide consistent error handling with structured JSON errors.
     *
     * - Returns AuthError instances with their errorDescription
     * - Returns exceptions with JSON-formatted error messages (wrapped AuthErrors) as-is
     * - Wraps unexpected exceptions in UnexpectedError with operation context
     *
     * @param e The exception to handle
     * @param operationName Name of the operation for error messages
     * @param jsCallback Callback to notify JavaScript caller with structured error
     */
    fun handleFunctionException(
        e: Exception,
        operationName: String,
        jsCallback: (com.geotab.mobile.sdk.module.Result<com.geotab.mobile.sdk.module.Success<String>, com.geotab.mobile.sdk.module.Failure>) -> Unit
    ) {
        when (e) {
            is AuthError -> {
                jsCallback(com.geotab.mobile.sdk.module.Failure(com.geotab.mobile.sdk.Error(com.geotab.mobile.sdk.models.enums.GeotabDriveError.AUTH_FAILED_ERROR, e.errorDescription)))
            }
            else -> {
                // Check if this is a wrapped structured error (JSON in exception message)
                val errorMessage = e.message
                if (AuthUtil.isStructuredAuthError(errorMessage)) {
                    // This is a structured error wrapped in a generic exception, pass it through as-is
                    jsCallback(com.geotab.mobile.sdk.module.Failure(com.geotab.mobile.sdk.Error(com.geotab.mobile.sdk.models.enums.GeotabDriveError.AUTH_FAILED_ERROR, errorMessage)))
                    return
                }

                // Truly unexpected exception - wrap it
                Logger.shared.error(TAG, "$operationName failed with unexpected error: ${e.message}", e)
                val unexpectedError = AuthError.UnexpectedError(
                    description = "$operationName failed with unexpected error",
                    underlyingError = e
                )
                jsCallback(com.geotab.mobile.sdk.module.Failure(com.geotab.mobile.sdk.Error(com.geotab.mobile.sdk.models.enums.GeotabDriveError.AUTH_FAILED_ERROR, unexpectedError.errorDescription)))
            }
        }
    }
}
