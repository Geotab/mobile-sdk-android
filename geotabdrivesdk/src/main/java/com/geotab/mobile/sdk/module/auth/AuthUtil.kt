package com.geotab.mobile.sdk.module.auth

import android.app.Activity
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
import androidx.work.WorkManager
import com.geotab.mobile.sdk.logging.Logger
import com.geotab.mobile.sdk.models.database.secureStorage.SecureStorageRepository
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.login.TokenRefreshWorker
import com.geotab.mobile.sdk.util.JsonUtil
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.EndSessionRequest
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import java.io.CharArrayReader
import kotlinx.parcelize.Parcelize

@Keep
data class AuthToken(
    val accessToken: String,
    val refreshToken: String? = null,
    val idToken: String? = null
)

@Parcelize
data class GeotabAuthState(
    val authState: String,
    val username: String,
    val ephemeralSession: Boolean = false
) : Parcelable

class AuthUtil(
    private val getValueChars: suspend (String) -> CharArray?,
    private val insertOrUpdate: suspend (String, CharArray) -> Unit,
    private val jsonUtil: JsonUtil = JsonUtil,
    @get:VisibleForTesting
    internal val authScope: CoroutineScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()),
    @get:VisibleForTesting
    internal val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    @get:VisibleForTesting
    internal val authCoordinator: AuthorizationCoordinator = AuthorizationCoordinator()
) {
    private var loginCallback: ((Result<Success<String>, Failure>) -> Unit)? = null
    private var logoutCallback: ((Result<Success<String>, Failure>) -> Unit)? = null
    var authService: AuthorizationService? = null
    private var authState: AuthState? = null
    private var currentGeotabAuthState: GeotabAuthState? = null
    private var currentEphemeralSession: Boolean = false

    // TODO: When LoginModule is removed, we can remove this flag as well
    private var isFromLoginModule = false

    // Token refresh retry tracking
    private val retryAttempts: MutableMap<String, Int> = mutableMapOf()

    // Store pending auth metadata for the current launch
    private var pendingAuthMetadata: AuthMetadata? = null

    /**
     * Stores username and flow type for an OAuth request in progress.
     * Used to provide error context to Sentry when OAuth fails.
     */
    data class AuthMetadata(
        val flowType: Auth.FlowType,
        val username: String
    )

    companion object {
        private const val TAG = "AuthUtil"
        private const val AUTH_TOKENS = "authTokens"
        private const val DEFAULT_BASE_RETRY_INTERVAL_MS = 2 * 60 * 1000L // 2 minutes
        private const val DEFAULT_MAX_RETRY_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes

        /**
         * Checks if an error message is a structured JSON error from AppAuth.
         * AppAuth errors are formatted as JSON objects containing a "code" field.
         *
         * @param errorMessage The error message to check
         * @return true if the error message is a structured JSON error, false otherwise
         */
        fun isStructuredAuthError(errorMessage: String?): Boolean {
            return errorMessage != null && errorMessage.startsWith("{") && errorMessage.contains("\"code\"")
        }

        @Volatile
        private var instance: AuthUtil? = null
        fun init(repository: SecureStorageRepository): AuthUtil {
            return instance ?: synchronized(this) {
                instance ?: AuthUtil(
                    getValueChars = repository::getValueChars,
                    insertOrUpdate = repository::insertOrUpdate
                ).also { instance = it }
            }
        }

        fun getInstance(): AuthUtil =
            instance
                ?: throw IllegalStateException("AuthUtil not initialized. Call init() in your Application class.")
    }

    private lateinit var loginActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var logoutActivityResultLauncher: ActivityResultLauncher<Intent>

    fun activityResultLauncherFunction(
        activityForResult: ComponentActivity
    ) {
        loginActivityResultLauncher = activityForResult.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // Retrieve and clear metadata
            val metadata = pendingAuthMetadata
            pendingAuthMetadata = null

            authScope.launch {
                handleAuthorizationResponse(
                    context = activityForResult.applicationContext,
                    data = result.data,
                    username = metadata?.username,
                    flowType = metadata?.flowType
                )
            }
        }
    }

    fun logoutActivityResultLauncherFunction(
        activityForResult: ComponentActivity
    ) {
        logoutActivityResultLauncher = activityForResult.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            authScope.launch {
                // Get context from the activity - this is available here
                val context = activityForResult.applicationContext

                if (result.resultCode == Activity.RESULT_OK) {
                    handleLogoutResponse(context)
                } else {
                    // Extract username from authState before clearing it (if still available)
                    val username = authState?.lastAuthorizationResponse?.request?.loginHint

                    sendAuthErrorMessage(
                        authError = AuthError.UserCancelledFlow,
                        callback = logoutCallback,
                        context = context,
                        username = username,
                        flowType = Auth.FlowType.LOGOUT
                    )
                }
            }
        }
    }

    /**
     * Perform login for a user.
     *
     * Uses suspend/throw pattern instead of callbacks:
     * - Suspends until login completes
     * - Throws exception on failure
     * - Returns AuthToken on success
     *
     * Multiple concurrent calls for the same user will be deduplicated by AuthorizationCoordinator,
     * with all callers receiving the same result.
     *
     * @param context Application context for error logging
     * @param clientId OAuth client ID
     * @param discoveryUri OAuth discovery URI
     * @param username The username to login
     * @param redirectScheme OAuth redirect URI
     * @param ephemeralSession Whether this is an ephemeral session
     * @param comingFromLoginModule Whether this call is from LoginModule (temporary flag)
     * @return AuthToken on successful login
     * @throws Exception on login failure
     */
    suspend fun login(
        context: Context,
        clientId: String,
        discoveryUri: Uri,
        username: String,
        redirectScheme: Uri,
        ephemeralSession: Boolean = false,
        comingFromLoginModule: Boolean = false
    ): AuthToken = withContext(authScope.coroutineContext) {
        return@withContext authCoordinator.performLogin(username) {
            performLoginInternal(
                context,
                clientId,
                discoveryUri,
                username,
                redirectScheme,
                ephemeralSession,
                comingFromLoginModule
            )
        }
    }

    private suspend fun performLoginInternal(
        context: Context,
        clientId: String,
        discoveryUri: Uri,
        username: String,
        redirectScheme: Uri,
        ephemeralSession: Boolean,
        comingFromLoginModule: Boolean
    ): AuthToken {
        return suspendCancellableCoroutine { continuation ->
            this.loginCallback = { result ->
                when (result) {
                    is Success -> {
                        val authToken = jsonUtil.fromJson<AuthToken>(result.value)
                        continuation.resume(authToken)
                    }

                    is Failure -> {
                        continuation.resumeWithException(result.reason)
                    }
                }
            }

            authScope.launch {
                Logger.shared.debug("$TAG.login", "Starting login for user: $username")
                isFromLoginModule = comingFromLoginModule
                currentEphemeralSession = ephemeralSession
                // Store metadata BEFORE launching
                pendingAuthMetadata = AuthMetadata(Auth.FlowType.LOGIN, username)
                try {
                    val serviceConfiguration = fetchFromUrlSuspend(discoveryUri)

                    val authRequest = AuthorizationRequest.Builder(
                        serviceConfiguration,
                        clientId,
                        ResponseTypeValues.CODE,
                        redirectScheme
                    )
                        .setScope("openid profile email")
                        .setLoginHint(username)
                        .build()

                    authService?.let { authService ->
                        val authIntent = authService.getAuthorizationRequestIntent(authRequest)
                        withContext(mainDispatcher) {
                            loginActivityResultLauncher.launch(authIntent)
                        }
                    } ?: throw IllegalStateException("AuthorizationService not initialized")
                } catch (e: ActivityNotFoundException) {
                    pendingAuthMetadata = null
                    sendAuthErrorMessage(
                        authError = AuthError.UnexpectedError(
                            description = "No browser available to handle authentication",
                            underlyingError = e
                        ),
                        callback = this@AuthUtil.loginCallback,
                        context = context,
                        username = username,
                        flowType = Auth.FlowType.LOGIN
                    )
                } catch (e: Exception) {
                    pendingAuthMetadata = null
                    sendAuthErrorMessage(
                        authError = AuthError.from(e, "Login failed with unexpected error"),
                        callback = this@AuthUtil.loginCallback,
                        context = context,
                        username = username,
                        flowType = Auth.FlowType.LOGIN
                    )
                }
            }
        }
    }

    /**
     * Perform authorization flow with a pre-fetched configuration.
     */
    private suspend fun performAuthorizationFlow(
        context: Context,
        configuration: AuthorizationServiceConfiguration,
        clientId: String,
        username: String,
        redirectScheme: Uri,
        ephemeralSession: Boolean,
        comingFromLoginModule: Boolean
    ): AuthToken {
        return suspendCancellableCoroutine { continuation ->
            this.loginCallback = { result ->
                when (result) {
                    is Success -> {
                        val authToken = jsonUtil.fromJson<AuthToken>(result.value)
                        continuation.resume(authToken)
                    }

                    is Failure -> {
                        continuation.resumeWithException(result.reason)
                    }
                }
            }

            authScope.launch {
                Logger.shared.debug("$TAG.performAuthorizationFlow", "Starting authorization flow for user: $username")
                isFromLoginModule = comingFromLoginModule
                currentEphemeralSession = ephemeralSession

                // Store metadata BEFORE launching
                pendingAuthMetadata = AuthMetadata(Auth.FlowType.REAUTH, username)
                try {
                    val authRequest = AuthorizationRequest.Builder(
                        configuration,
                        clientId,
                        ResponseTypeValues.CODE,
                        redirectScheme
                    )
                        .setScope("openid profile email")
                        .setLoginHint(username)
                        .build()

                    authService?.let { authService ->
                        val authIntent = authService.getAuthorizationRequestIntent(authRequest)
                        withContext(mainDispatcher) {
                            loginActivityResultLauncher.launch(authIntent)
                        }
                    } ?: throw IllegalStateException("AuthorizationService not initialized")
                } catch (e: ActivityNotFoundException) {
                    pendingAuthMetadata = null
                    sendAuthErrorMessage(
                        authError = AuthError.UnexpectedError(
                            description = "No browser available to handle re-authentication",
                            underlyingError = e
                        ),
                        callback = this@AuthUtil.loginCallback,
                        context = context,
                        username = username,
                        flowType = Auth.FlowType.REAUTH
                    )
                } catch (e: Exception) {
                    pendingAuthMetadata = null
                    sendAuthErrorMessage(
                        authError = AuthError.from(e, "Re-authentication failed with unexpected error"),
                        callback = this@AuthUtil.loginCallback,
                        context = context,
                        username = username,
                        flowType = Auth.FlowType.REAUTH
                    )
                }
            }
        }
    }

    /**
     * Re-authenticate an existing user using their stored auth configuration.
     *
     * This method is used when a token refresh fails due to the refresh token being invalid
     * or revoked by the auth server. It reuses the existing OAuth configuration and settings
     * from the user's previous login.
     *
     * Unlike login(), this method uses suspend/throw pattern:
     * - Suspends until re-authentication completes
     * - Throws AuthError or GetTokenError on failure
     * - Returns AuthToken on success
     *
     * If user cancels the reauth flow, this method will:
     * - Delete auth state from storage
     * - Cancel any scheduled background token refresh workers
     * - Throw the cancellation error
     *
     * @param context Application context for cleanup operations
     * @param username The username to re-authenticate
     * @return AuthToken on successful re-authentication
     * @throws GetTokenError.NoAccessTokenFoundError if no existing auth state found
     * @throws AuthError.SessionRetrieveFailedError if unable to extract configuration
     */
    suspend fun reauth(
        context: Context,
        username: String
    ): AuthToken = withContext(authScope.coroutineContext) {
        return@withContext authCoordinator.performReauth(username) {
            Logger.shared.debug("$TAG.reauth", "Starting re-authentication for user: $username")

            try {
                // Load existing auth state to get configuration and client info
                getAuthState(username)
                val state = authState ?: throw AuthError.NoAccessTokenFoundError(username)
                val geotabState = currentGeotabAuthState ?: throw AuthError.NoAccessTokenFoundError(username)

                // Extract OAuth configuration from stored auth state
                val authRequest = state.lastAuthorizationResponse?.request
                    ?: throw AuthError.SessionRetrieveFailedError

                val configuration = authRequest.configuration
                val clientId = authRequest.clientId
                val redirectUri = authRequest.redirectUri
                val ephemeralSession = geotabState.ephemeralSession

                // Perform re-authentication using stored configuration
                performAuthorizationFlow(
                    context = context,
                    configuration = configuration,
                    clientId = clientId,
                    username = username,
                    redirectScheme = redirectUri,
                    ephemeralSession = ephemeralSession,
                    comingFromLoginModule = false
                )
            } catch (e: Exception) {
                // Check if user cancelled the reauth flow
                if (e is AuthError.UserCancelledFlow) {
                    // Clear all saved tokens and cancel background refresh for this user
                    Logger.shared.info("$TAG.reauth", "User cancelled reauth, clearing tokens and canceling refresh worker for $username")
                    try {
                        deleteToken(context, username)
                    } catch (deleteError: Exception) {
                        Logger.shared.error("$TAG.reauth", "Failed to cleanup after user cancellation: ${deleteError.message}", deleteError)
                    }
                }
                throw e
            }
        }
    }

    /**
     * Perform logout for a user.
     *
     * @param context Application context
     * @param username The username to logout
     * @throws Exception on logout failure
     */
    suspend fun logout(
        context: Context,
        username: String
    ) = withContext(authScope.coroutineContext) {
        authCoordinator.performLogout(username) {
            Logger.shared.debug("$TAG.logout", "Starting logout for user: $username")

            try {
                getAuthState(username)

                if (authState == null) {
                    throw AuthError.NoAccessTokenFoundError(username)
                }

                revokeToken()
                launchLogoutUser()
            } catch (e: ActivityNotFoundException) {
                val authError = AuthError.UnexpectedError(
                    description = "Failed to launch logout browser",
                    underlyingError = e
                )
                sendAuthErrorMessage(
                    authError = authError,
                    callback = null, // No callback for logout
                    context = context,
                    username = username,
                    flowType = Auth.FlowType.LOGOUT
                )
                throw authError
            } catch (e: Exception) {
                // Convert to AuthError and log to Sentry if applicable
                val authError = if (e is AuthError) e else AuthError.from(e, "Logout failed")
                sendAuthErrorMessage(
                    authError = authError,
                    callback = null, // No callback for logout
                    context = context,
                    username = username,
                    flowType = Auth.FlowType.LOGOUT
                )
                throw authError
            } finally {
                deleteToken(context, username)
            }
        }
    }

    suspend fun getValidAccessToken(
        context: Context,
        username: String,
        forceRefresh: Boolean = false,
        startScheduler: Boolean = true
    ): AuthToken? = withContext(authScope.coroutineContext) {
        return@withContext authCoordinator.performTokenRefresh(username, forceRefresh) {
            getAuthState(username)
            val state = authState ?: return@performTokenRefresh null
            // Refresh if it's forced (by the worker) OR if the token is already expired.
            val needsRefresh = forceRefresh || state.needsTokenRefresh

            if (needsRefresh) {
                Logger.shared.debug("$TAG.getValidAccessToken", "Starting token refresh for user: $username")
                try {
                    val tokenRefreshRequest = state.createTokenRefreshRequest()

                    val refreshedTokenResponse =
                        authService?.performTokenRequestSuspend(tokenRefreshRequest)
                            ?: throw Exception("Token refresh resulted in a null response")

                    // Update the in-memory state object with the new token
                    state.update(refreshedTokenResponse, null)
                    // Persist the updated state to the database, preserving ephemeralSession
                    insertToken(
                        GeotabAuthState(
                            authState = state.jsonSerializeString(),
                            username = username.lowercase(),
                            ephemeralSession = currentGeotabAuthState?.ephemeralSession ?: false
                        )
                    )
                    // Reset retry attempts after successful refresh
                    resetRetryAttempts(username)
                    if (startScheduler) {
                        // Reschedule the worker after successful refresh
                        rescheduleTokenRefreshWorker(context, username)
                    }
                } catch (ex: Exception) {
                    val flowType = when {
                        forceRefresh && (retryAttempts[username] ?: 0) > 0 -> Auth.FlowType.BACKGROUND_REFRESH_RETRY

                        forceRefresh -> Auth.FlowType.BACKGROUND_REFRESH
                        else -> Auth.FlowType.TOKEN_REFRESH
                    }
                    val isRecoverable = AuthError.isRecoverableError(ex)
                    when {
                        // Path 1: Recoverable error (network issue) - keep auth state, user can retry
                        isRecoverable -> {
                            Logger.shared.info("$TAG.getValidAccessToken", "Token refresh failed (recoverable): ${ex.message}")

                            val error = AuthError.TokenRefreshFailed(
                                username = username,
                                underlyingError = ex,
                                requiresReauthentication = false
                            )

                            sendAuthErrorMessage(
                                authError = error,
                                callback = null,
                                context = context,
                                username = username,
                                flowType = flowType,
                                additionalContext = mapOf(Auth.ContextKey.RECOVERABLE to true)
                            )

                            throw error
                        }

                        // Path 2: Non-recoverable error, app in background - can't show reauth UI
                        !isRecoverable && isAppInBackground(context) -> {
                            Logger.shared.info("$TAG.getValidAccessToken", "Token refresh failed (requires re-auth): ${ex.message}")
                            Logger.shared.info("$TAG.getValidAccessToken", "App is in background, deferring reauth until foreground")

                            val error = AuthError.TokenRefreshFailed(
                                username = username,
                                underlyingError = ex,
                                requiresReauthentication = true
                            )

                            sendAuthErrorMessage(
                                authError = error,
                                callback = null,
                                context = context,
                                username = username,
                                flowType = flowType,
                                additionalContext = mapOf(
                                    Auth.ContextKey.RECOVERABLE to false,
                                    Auth.ContextKey.REQUIRES_REAUTH to true
                                )
                            )

                            throw error
                        }

                        // Path 3: Non-recoverable error, app in foreground - trigger reauth now
                        !isRecoverable && !isAppInBackground(context) -> {
                            Logger.shared.info("$TAG.getValidAccessToken", "Token refresh failed (requires re-auth): ${ex.message}")

                            val error = AuthError.from(ex, "Token refresh failed - triggering reauth")

                            sendAuthErrorMessage(
                                authError = error,
                                callback = null,
                                context = context,
                                username = username,
                                flowType = flowType,
                                additionalContext = mapOf(Auth.ContextKey.REQUIRES_REAUTH to true)
                            )

                            return@performTokenRefresh reauth(context, username)
                        }
                    }
                }
            }
            state.accessToken?.takeIf { it.isNotEmpty() }?.let { AuthToken(it) }
        }
    }

    internal suspend fun handleAuthorizationResponse(
        context: Context,
        data: Intent?,
        username: String?,
        flowType: Auth.FlowType?
    ) {
        // Extract response and exception
        val response = data?.let { AuthorizationResponse.fromIntent(it) }
        val exception = data?.let { AuthorizationException.fromIntent(it) }

        if (data == null) {
            sendAuthErrorMessage(
                authError = AuthError.UserCancelledFlow,
                callback = loginCallback,
                context = context,
                username = username,
                flowType = flowType
            )
            return
        }

        when {
            exception != null -> {
                // Convert AuthorizationException to appropriate AuthError type
                val authError = AuthError.from(exception, "Authorization failed")
                sendAuthErrorMessage(
                    authError = authError,
                    callback = loginCallback,
                    context = context,
                    username = username,
                    flowType = flowType
                )
            }
            response == null -> {
                sendAuthErrorMessage(
                    authError = AuthError.NoDataFoundError,
                    callback = loginCallback,
                    context = context,
                    username = username,
                    flowType = flowType
                )
            }
            else -> {
                try {
                    val authService = this.authService ?: throw IllegalStateException("AuthorizationService not initialized")
                    val tokenRequest = response.createTokenExchangeRequest()
                    val tokenResponse = authService.performTokenRequestSuspend(tokenRequest)
                    handleSuccessfulTokenExchange(context, response, tokenResponse)
                } catch (e: Exception) {
                    sendAuthErrorMessage(
                        authError = AuthError.from(e, "Token exchange failed with unexpected error"),
                        callback = loginCallback,
                        context = context,
                        username = username,
                        flowType = flowType
                    )
                }
            }
        }
    }

    /**
     * Sends an auth error message and optionally logs to Sentry.
     * Centralizes error handling for all auth operations (login, reauth, token refresh, logout).
     *
     * @param authError The authentication error that occurred
     * @param callback The callback to invoke with the error
     * @param context Application context for app state detection (optional, required for Sentry logging)
     * @param username The username associated with the auth operation (optional, defaults to "unknown" for Sentry)
     * @param flowType The type of auth flow (optional, required for Sentry logging)
     * @param additionalContext Optional additional context for Sentry logging
     */
    private fun sendAuthErrorMessage(
        authError: AuthError,
        callback: ((Result<Success<String>, Failure>) -> Unit)? = null,
        context: Context? = null,
        username: String? = null,
        flowType: Auth.FlowType? = null,
        additionalContext: Map<Auth.ContextKey, Any>? = null
    ) {
        // Capture unexpected errors in Sentry with structured context
        if (context != null && flowType != null && AuthError.shouldBeCaptured(authError)) {
            // Use authFailure for Sentry capture - this will also log to Logcat
            Logger.shared.authFailure(
                context = context,
                username = username ?: "unknown",
                flowType = flowType,
                error = authError,
                additionalContext = additionalContext
            )
        } else {
            // Expected operational errors: just log to Logcat, not Sentry
            val logMessage = buildString {
                append("Auth failed")
                if (username != null) {
                    append(" for user $username")
                }
                if (flowType != null) {
                    append(" (${flowType.value})")
                }
                append(": ${authError.fallbackErrorMessage}")
            }
            Logger.shared.warn(
                "$TAG.sendAuthErrorMessage",
                logMessage
            )
        }

        callback?.let {
            it(Failure(authError))
        }
    }

    private suspend fun handleSuccessfulTokenExchange(
        context: Context,
        authResponse: AuthorizationResponse,
        tokenResponse: TokenResponse
    ) {
        // Create temporary auth state for validation - don't set global authState yet
        val tempAuthState = AuthState(authResponse, tokenResponse, null)

        val username = authResponse.request.loginHint
        val flowType =
            if (authResponse.request.state == null) Auth.FlowType.LOGIN else Auth.FlowType.REAUTH

        if (username == null) {
            sendAuthErrorMessage(
                authError = AuthError.MissingAuthData,
                callback = loginCallback,
                context = context,
                username = null,
                flowType = flowType
            )
            return
        }

        // Validate username matches the one in the access token
        // If validation fails, performUsernameValidation will send error via callback and return early
        // Pass tempAuthState so it can be revoked if validation fails, without affecting the global authState
        if (!performUsernameValidation(username, tokenResponse.accessToken, context, tempAuthState, flowType)) {
            return // Global authState remains unchanged (previous user or null)
        }

        // Validation passed - now it's safe to update the global authState
        authState = tempAuthState
        authState?.update(tokenResponse, null)

        loginCallback?.let {
            // TODO: When LoginModule is removed, we can remove this check
            val authToken = if (isFromLoginModule) {
                AuthToken(
                    accessToken = tokenResponse.accessToken ?: "",
                    refreshToken = tokenResponse.refreshToken ?: "",
                    idToken = tokenResponse.idToken ?: ""
                )
            } else {
                AuthToken(accessToken = tokenResponse.accessToken ?: "")
            }

            it(Success(JsonUtil.toJson(authToken)))
        }

        val geotabAuthState = GeotabAuthState(
            authState = authState?.jsonSerializeString() ?: "",
            username = username.lowercase(),
            ephemeralSession = currentEphemeralSession
        )

        if (!isFromLoginModule) {
            insertToken(geotabAuthState)
            resetRetryAttempts(username)
            rescheduleTokenRefreshWorker(context, geotabAuthState.username)
        }
    }

    private suspend fun getAuthState(username: String) {
        try {
            val tokensList = getAllTokens()
            val geotabAuthState = getCredentialsFromUsername(username, tokensList) ?: run {
                Logger.shared.error(
                    "$TAG.getAuthState",
                    "No auth state found for user: $username"
                )

                // Since we couldn't find a token for the user, ensure authState is null
                authState = null
                currentGeotabAuthState = null
                return
            }

            authState = AuthState.jsonDeserialize(geotabAuthState.authState)
            currentGeotabAuthState = geotabAuthState
        } catch (e: Exception) {
            Logger.shared.error(
                "$TAG.getAuthState",
                "Error fetching auth state: ${e.message ?: "Unknown error"}",
                e
            )
        }
    }

    private suspend fun AuthorizationService.performTokenRequestSuspend(
        request: TokenRequest
    ): TokenResponse = suspendCancellableCoroutine { continuation ->
        performTokenRequest(request) { response, ex ->
            when {
                continuation.isCancelled -> return@performTokenRequest
                response != null -> continuation.resume(response)
                ex != null -> continuation.resumeWithException(ex)
                else -> continuation.resumeWithException(
                    Exception("Unexpected state: response and exception are null")
                )
            }
        }
    }

    private suspend fun fetchFromUrlSuspend(
        uri: Uri
    ): AuthorizationServiceConfiguration = suspendCancellableCoroutine { continuation ->
        AuthorizationServiceConfiguration.fetchFromUrl(uri) { config, ex ->
            when {
                continuation.isCancelled -> return@fetchFromUrl
                config != null -> continuation.resume(config)
                ex != null -> continuation.resumeWithException(ex)
                else -> continuation.resumeWithException(Exception("Unexpected state: config and exception are null"))
            }
        }
    }

    private suspend fun insertToken(
        geotabAuthState: GeotabAuthState
    ) {
        val tokensList = getAllTokens()

        val existingToken = getCredentialsFromUsername(geotabAuthState.username, tokensList)

        if (existingToken != null) {
            tokensList.removeIf { it.username == existingToken.username }
        }

        tokensList.add(geotabAuthState)

        val geotabAuthStateJson = JsonUtil.toJson(tokensList).toCharArray()
        insertOrUpdate(AUTH_TOKENS, geotabAuthStateJson)
    }

    private suspend fun deleteToken(
        context: Context,
        username: String
    ) {
        try {
            val tokensList = getAllTokens()

            tokensList.removeIf { it.username == username.lowercase() }

            val geotabAuthStateChars = JsonUtil.toJson(tokensList).toCharArray()
            insertOrUpdate(AUTH_TOKENS, geotabAuthStateChars)
            cancelScheduleNextRefreshToken(context, username)
        } catch (e: Exception) {
            Logger.shared.error(
                "$TAG.deleteToken",
                "Error deleting auth token: ${e.message ?: "Unknown error"}",
                e
            )
        }
    }

    private fun getCredentialsFromUsername(username: String, tokensList: MutableList<GeotabAuthState>): GeotabAuthState? {
        val normalizedUsername = username.lowercase()
        return tokensList.firstOrNull { it.username == normalizedUsername }
    }

    internal fun calculateNextRefreshDelay(): Long {
        if (authState == null) {
            return 0
        }
        val expirationTime = authState?.accessTokenExpirationTime ?: return 0
        val now = System.currentTimeMillis()
        val expiresIn = expirationTime - now
        // Return 0 for expired tokens
        if (expiresIn <= 0) return 0

        val minDelayMs = 2 * 60 * 1000L // 2 minutes in milliseconds
        val calculatedDelay = expiresIn / 2
        return if (calculatedDelay > minDelayMs) calculatedDelay else minDelayMs
    }

    internal suspend fun getAllTokens(): MutableList<GeotabAuthState> {
        val allTokensChars = getValueChars(AUTH_TOKENS)

        val tokensList = if (allTokensChars?.isNotEmpty() == true) {
            try {
                JsonUtil.fromJson<MutableList<GeotabAuthState>>(CharArrayReader(allTokensChars))
            } catch (e: Exception) {
                Logger.shared.error(
                    "$TAG.getAllTokens",
                    "Error parsing auth tokens JSON: ${e.message ?: "Unknown error"}",
                    e
                )
                mutableListOf()
            } finally {
                allTokensChars.fill('\u0000')
            }
        } else {
            mutableListOf()
        }
        return tokensList
    }

    internal fun rescheduleTokenRefreshWorker(context: Context, username: String) {
        cancelScheduleNextRefreshToken(context, username)
        scheduleNextRefreshToken(context, username)
    }

    internal fun cancelScheduleNextRefreshToken(context: Context, username: String) {
        val uniqueWorkName = TokenRefreshWorker.getUniqueWorkName(username)
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)
    }

    internal fun scheduleNextRefreshToken(context: Context, username: String) {
        val delay = calculateNextRefreshDelay()
        if (delay > 0) {
            TokenRefreshWorker.scheduleTokenRefreshWorker(context, username, delay)
        }
    }

    /**
     * Calculates exponential backoff delay based on retry attempts.
     * Formula: base interval * 2^attempt, capped at max interval.
     *
     * Backoff progression:
     * - Attempt 0: 2 minutes
     * - Attempt 1: 4 minutes
     * - Attempt 2: 8 minutes
     * - Attempt 3+: 15 minutes (capped, retries continue indefinitely at this interval)
     *
     * Retries continue forever for recoverable errors (network issues) until:
     * - Token refresh succeeds
     * - Error becomes unrecoverable (invalid token triggers reauth)
     * - User manually logs out
     *
     * @param attempt The current retry attempt number (0-indexed)
     * @return Delay in milliseconds before the next retry
     */
    internal fun calculateBackoffDelay(attempt: Int): Long {
        val exponentialDelay = DEFAULT_BASE_RETRY_INTERVAL_MS * Math.pow(2.0, attempt.toDouble()).toLong()
        return minOf(exponentialDelay, DEFAULT_MAX_RETRY_INTERVAL_MS)
    }

    /**
     * Schedules the next token refresh with exponential backoff for retry scenarios.
     * Increments retry attempt count and schedules worker with backoff delay.
     */
    internal fun scheduleNextRefreshTokenWithBackoff(context: Context, username: String) {
        val currentAttempt = retryAttempts[username] ?: 0
        retryAttempts[username] = currentAttempt + 1

        val retryDelay = calculateBackoffDelay(currentAttempt)
        Logger.shared.debug(
            TAG,
            "Scheduling retry for $username in ${retryDelay / 1000} seconds (attempt ${currentAttempt + 1})"
        )
        TokenRefreshWorker.scheduleTokenRefreshWorker(context, username, retryDelay)
    }

    /**
     * Resets retry attempts for a user after successful token refresh.
     */
    internal fun resetRetryAttempts(username: String) {
        retryAttempts.remove(username)
    }

    private suspend fun launchLogoutUser() {
        val config = authState?.authorizationServiceConfiguration
        val redirectUri = authState?.lastAuthorizationResponse?.request?.redirectUri
        val idToken = authState?.idToken

        if (config == null || redirectUri == null || idToken == null) {
            authState = null
            Logger.shared.error("$TAG.launchLogoutUser", "No valid configuration, redirect URI, or ID token found in logout flow")
            return
        }

        val endSessionRequest = EndSessionRequest.Builder(config)
            .setIdTokenHint(idToken)
            .setPostLogoutRedirectUri(redirectUri)
            .build()

        val endSessionIntent = authService?.getEndSessionRequestIntent(endSessionRequest)

        withContext(mainDispatcher) {
            logoutActivityResultLauncher.launch(endSessionIntent)
        }
    }

    internal fun handleLogoutResponse(context: Context) {
        val username = authState?.lastAuthorizationResponse?.request?.loginHint

        try {
            authState = null

            logoutCallback?.let {
                val message = "Logged out successfully"
                Logger.shared.info("$TAG.handleLogoutResponse", message)
                it(Success(jsonUtil.toJson(message)))
            }
        } catch (ex: Exception) {
            sendAuthErrorMessage(
                authError = when (ex) {
                    is AuthError -> ex
                    else -> AuthError.RevokeTokenFailed(ex)
                },
                callback = logoutCallback,
                context = context,
                username = username,
                flowType = Auth.FlowType.LOGOUT
            )
        }
    }

    /**
     * Revokes the current tokens by making a manual POST request to the revocation endpoint
     * found in the discovery document.
     *
     * @param stateToRevoke The AuthState containing the tokens to revoke. Defaults to the current authState.
     */
    private fun revokeToken(stateToRevoke: AuthState? = authState) {
        val discoveryDoc = stateToRevoke?.authorizationServiceConfiguration?.discoveryDoc
        val revocationEndpoint = try {
            discoveryDoc?.docJson?.getString("revocation_endpoint")
        } catch (e: Exception) {
            Logger.shared.error("$TAG.revokeToken", "No 'revocation_endpoint' found in discovery document.", e)
            null
        }

        if (revocationEndpoint.isNullOrEmpty()) {
            Logger.shared.error("$TAG.revokeToken", "Authorization server configuration does not support token revocation.")
            return
        }

        val tokenToRevoke = stateToRevoke?.refreshToken ?: stateToRevoke?.accessToken
        if (tokenToRevoke == null) {
            Logger.shared.debug("$TAG.revokeToken", "No valid token available to revoke.")
            return
        }

        var connection: HttpURLConnection? = null
        try {
            val revocationUrl = URL(revocationEndpoint)
            connection = revocationUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val clientId = stateToRevoke?.lastAuthorizationResponse?.request?.clientId
            if (clientId.isNullOrEmpty()) {
                Logger.shared.error("$TAG.revokeToken", "Client ID is missing, cannot perform revocation.")
                return
            }

            connection.doOutput = true
            val requestBody = "token=$tokenToRevoke&client_id=$clientId"
            val writer = BufferedWriter(OutputStreamWriter(connection.outputStream, "UTF-8"))
            writer.write(requestBody)
            writer.flush()
            writer.close()

            Logger.shared.info("$TAG.revokeToken", "Token revocation response code: ${connection.responseCode}")
        } catch (e: IOException) {
            Logger.shared.error("$TAG.revokeToken", "Network error during token revocation.", e)
        } catch (e: Exception) {
            Logger.shared.error("$TAG.revokeToken", "An unexpected error occurred during token revocation.", e)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun startTokenRefresh(context: Context) {
        val workManager = WorkManager.getInstance(context)
        try {
            // Get all stored user tokens
            val allTokens = getAllTokens()
            // Iterate and cancel existing work, then schedule new work
            allTokens.forEach { geotabAuthState ->
                val workName = TokenRefreshWorker.getUniqueWorkName(geotabAuthState.username)
                workManager.cancelUniqueWork(workName)
                getValidAccessToken(context, geotabAuthState.username, forceRefresh = true)
            }
        } catch (e: Exception) {
            Logger.shared.error(
                "$TAG.startTokenRefresh",
                "Error starting token refresh: ${e.message}",
                e
            )
        }
    }

    /**
     * Checks if the app is currently in the background.
     * Returns true if no activities are visible to the user.
     */
    internal fun isAppInBackground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return false

        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = context.packageName
        val myPid = Process.myPid()

        return appProcesses.firstOrNull { it.pid == myPid }?.let { processInfo ->
            processInfo.processName == packageName &&
                processInfo.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        } ?: false
    }

    /**
     * Performs username validation during login flow.
     * Validates only if username is not empty and access token is present.
     * Sends error message and returns false if validation fails.
     *
     * @param username The expected username from the login request
     * @param accessToken The JWT access token to validate (can be null)
     * @param context Application context for cleanup operations
     * @param authStateToValidate The AuthState being validated (will be revoked if validation fails)
     * @param flowType The type of auth flow for error logging
     * @return true if validation passed or was skipped, false if validation failed
     */
    private suspend fun performUsernameValidation(
        username: String,
        accessToken: String?,
        context: Context,
        authStateToValidate: AuthState,
        flowType: Auth.FlowType
    ): Boolean {
        // Skip validation if username is empty or access token is missing
        if (username.isEmpty() || accessToken == null) {
            return true
        }

        return try {
            validateUsername(
                expected = username,
                accessToken = accessToken,
                context = context,
                authStateToValidate = authStateToValidate
            )
            true
        } catch (e: AuthError) {
            sendAuthErrorMessage(
                authError = e,
                callback = loginCallback,
                context = context,
                username = username,
                flowType = flowType
            )
            // Return false to signal validation failed
            // Don't re-throw - sendAuthErrorMessage already resumes the continuation with error
            false
        }
    }

    /**
     * Validates that the username in the access token matches the expected username.
     * If they don't match, deletes auth state and revokes tokens before throwing an error.
     *
     * @param expected The expected username from the login request
     * @param accessToken The JWT access token to validate
     * @param context Application context for cleanup operations
     * @param authStateToValidate The AuthState being validated (will be revoked if validation fails)
     * @throws AuthError.UsernameMismatch if usernames don't match
     * @throws AuthError.ParseFailedError if JWT parsing fails
     */
    @VisibleForTesting
    internal suspend fun validateUsername(
        expected: String,
        accessToken: String,
        context: Context,
        authStateToValidate: AuthState
    ) = withContext(authScope.coroutineContext) {
        val actualUsername = extractUsername(accessToken)
            ?: throw AuthError.ParseFailedError

        // Normalize both usernames for case-insensitive comparison
        val normalizedExpected = expected.lowercase()
        val normalizedActual = actualUsername.lowercase()

        if (normalizedActual != normalizedExpected) {
            cleanupAfterValidationFailure(expected, context, authStateToValidate)
            throw AuthError.UsernameMismatch(expected = normalizedExpected, actual = normalizedActual)
        }
    }

    /**
     * Cleans up auth state and revokes tokens after username validation failure.
     *
     * @param username The username to clean up
     * @param context Application context for cleanup operations
     * @param failedAuthState The AuthState that failed validation (to be revoked)
     */
    private suspend fun cleanupAfterValidationFailure(
        username: String,
        context: Context,
        failedAuthState: AuthState
    ) {
        try {
            deleteToken(context, username)
        } catch (e: Exception) {
            Logger.shared.error("$TAG.cleanupAfterValidationFailure", "Failed to delete auth state: ${e.message}", e)
        }

        try {
            revokeToken(failedAuthState)
        } catch (e: Exception) {
            Logger.shared.error("$TAG.cleanupAfterValidationFailure", "Failed to revoke token: ${e.message}", e)
        }
    }

    /**
     * Extracts the username from a JWT access token.
     * Tries to extract from preferred_username, email, or sub claims in that order.
     *
     * @param jwt The JWT token string
     * @return The username extracted from the token, or null if parsing fails
     */
    @VisibleForTesting
    internal fun extractUsername(jwt: String): String? {
        val parts = jwt.split(".")
        if (parts.size < 2) {
            return null
        }

        val payloadPart = parts[1]
        val payloadData = base64UrlDecode(payloadPart) ?: return null

        return try {
            val payload = String(payloadData, Charsets.UTF_8)
            val json = JsonUtil.fromJson<Map<String, Any>>(payload)

            json["preferred_username"] as? String
                ?: json["email"] as? String
                ?: json["sub"] as? String
        } catch (e: Exception) {
            Logger.shared.error("$TAG.extractUsername", "Failed to parse JWT payload: ${e.message}", e)
            null
        }
    }

    /**
     * Decodes a base64url-encoded string.
     * Handles padding and URL-safe character replacement.
     *
     * @param base64Url The base64url-encoded string
     * @return The decoded byte array, or null if decoding fails
     */
    @VisibleForTesting
    internal fun base64UrlDecode(base64Url: String): ByteArray? {
        return try {
            var base64 = base64Url
                .replace("-", "+")
                .replace("_", "/")

            val paddingLength = (4 - base64.length % 4) % 4
            base64 += "=".repeat(paddingLength)

            android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            Logger.shared.error("$TAG.base64UrlDecode", "Failed to decode base64url: ${e.message}", e)
            null
        }
    }

    fun dispose(context: Context) {
        TokenRefreshWorker.cancelAllTokenRefreshWork(context)
        authService?.dispose()
        authService = null
    }
}
