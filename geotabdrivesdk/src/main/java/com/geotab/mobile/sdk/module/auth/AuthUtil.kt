package com.geotab.mobile.sdk.module.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
import androidx.work.WorkManager
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.logging.Logger
import com.geotab.mobile.sdk.models.database.secureStorage.SecureStorageRepository
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.login.LoginModule
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
    val username: String
) : Parcelable

class AuthUtil(
    private val getValueChars: suspend (String) -> CharArray?,
    private val insertOrUpdate: suspend (String, CharArray) -> Unit,
    private val jsonUtil: JsonUtil = JsonUtil,
    @get:VisibleForTesting
    internal val authScope: CoroutineScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()),
    @get:VisibleForTesting
    internal val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private var loginCallback: ((Result<Success<String>, Failure>) -> Unit)? = null
    private var logoutCallback: ((Result<Success<String>, Failure>) -> Unit)? = null
    var authService: AuthorizationService? = null
    private var authState: AuthState? = null

    // TODO: When LoginModule is removed, we can remove this flag as well
    private var isFromLoginModule = false

    companion object {
        private const val TAG = "AuthUtil"
        private const val AUTH_TOKENS = "authTokens"

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
            authScope.launch {
                handleAuthorizationResponse(activityForResult.applicationContext, result.data)
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
                if (result.resultCode == Activity.RESULT_OK) {
                    handleLogoutResponse()
                } else {
                    sendErrorMessage(
                        errorMessage = "Logout failed or was cancelled",
                        callback = logoutCallback
                    )
                }
            }
        }
    }

    fun login(
        clientId: String,
        discoveryUri: Uri,
        username: String,
        redirectScheme: Uri,
        comingFromLoginModule: Boolean = false,
        loginCallback: ((Result<Success<String>, Failure>) -> Unit)?
    ) {
        this.loginCallback = loginCallback
        authScope.launch {
            isFromLoginModule = comingFromLoginModule
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
                    .setState(username)
                    .build()

                authService?.let { authService ->
                    val authIntent = authService.getAuthorizationRequestIntent(authRequest)
                    withContext(mainDispatcher) {
                        loginActivityResultLauncher.launch(authIntent)
                    }
                } ?: throw IllegalStateException("AuthorizationService not initialized")
            } catch (e: Exception) {
                sendErrorMessage(
                    errorMessage = e.message ?: "Failed to fetch configuration or create auth request",
                    callback = loginCallback
                )
            }
        }
    }

    suspend fun logout(
        context: Context,
        username: String,
        logoutCallbackFromModule: ((Result<Success<String>, Failure>) -> Unit)?
    ) = withContext(authScope.coroutineContext) {
        logoutCallback = logoutCallbackFromModule

        try {
            getAuthState(username)

            if (authState == null) {
                logoutCallback?.let {
                    val message = "No valid token found for user $username"
                    Logger.shared.error(TAG, message)
                    it(Failure(Error(GeotabDriveError.AUTH_FAILED_ERROR, jsonUtil.toJson(message))))
                }
                return@withContext
            }

            revokeToken()
            launchLogoutUser()
        } catch (e: Exception) {
            sendErrorMessage(
                errorMessage = e.message ?: "Failed to create end session request",
                callback = logoutCallback
            )
        } finally {
            deleteToken(context, username)
        }
    }

    suspend fun getValidAccessToken(
        context: Context,
        username: String,
        forceRefresh: Boolean = false,
        startScheduler: Boolean = true
    ): AuthToken? = withContext(authScope.coroutineContext) {
        getAuthState(username)
        val state = authState ?: return@withContext null
        // Refresh if it's forced (by the worker) OR if the token is already expired.
        val needsRefresh = forceRefresh || state.needsTokenRefresh

        if (needsRefresh) {
            try {
                val tokenRefreshRequest = state.createTokenRefreshRequest()

                val refreshedTokenResponse =
                    authService?.performTokenRequestSuspend(tokenRefreshRequest)
                        ?: throw Exception("Token refresh resulted in a null response")

                // Update the in-memory state object with the new token
                state.update(refreshedTokenResponse, null)
                // Persist the updated state to the database
                insertToken(
                    GeotabAuthState(
                        authState = state.jsonSerializeString(),
                        username = username
                    )
                )
                if (startScheduler) {
                    // Reschedule the worker after successful refresh
                    rescheduleTokenRefreshWorker(context, username)
                }
            } catch (ex: Exception) {
                Logger.shared.error(TAG, "Exception on refresh: ${ex.message}")
                deleteToken(context, username)
                authState = null
                return@withContext null
            }
        }
        return@withContext state.accessToken?.takeIf { it.isNotEmpty() }?.let { AuthToken(it) }
    }

    internal suspend fun handleAuthorizationResponse(context: Context, data: Intent?) {
        if (data == null) {
            sendErrorMessage(
                errorMessage = "Activity result was null",
                callback = loginCallback
            )
            return
        }

        val response = AuthorizationResponse.fromIntent(data)
        val exception = AuthorizationException.fromIntent(data)

        when {
            exception != null -> {
                sendErrorMessage(
                    errorMessage = exception.message ?: "Authorization failed",
                    callback = loginCallback
                )
            }
            response == null -> {
                sendErrorMessage(
                    errorMessage = "No data returned from authorization flow",
                    callback = loginCallback
                )
            }
            else -> {
                try {
                    val authService = this.authService ?: throw IllegalStateException("AuthorizationService not initialized")
                    val tokenRequest = response.createTokenExchangeRequest()
                    val tokenResponse = authService.performTokenRequestSuspend(tokenRequest)
                    handleSuccessfulTokenExchange(context, response, tokenResponse)
                } catch (e: Exception) {
                    sendErrorMessage(
                        errorMessage = e.message ?: "Token exchange failed",
                        callback = loginCallback
                    )
                }
            }
        }
    }

    private fun sendErrorMessage(
        errorMessage: String,
        callback: ((Result<Success<String>, Failure>) -> Unit)? = loginCallback
    ) {
        Logger.shared.error(
            TAG,
            errorMessage
        )
        callback?.let {
            it(Failure(Error(GeotabDriveError.AUTH_FAILED_ERROR, errorMessage)))
        }
    }

    private suspend fun handleSuccessfulTokenExchange(
        context: Context,
        authResponse: AuthorizationResponse,
        tokenResponse: TokenResponse
    ) {
        authState = AuthState(authResponse, tokenResponse, null)

        val username = authResponse.state
        if (username == null) {
            sendErrorMessage(
                errorMessage = "Username (state) is null",
                callback = loginCallback
            )
            return
        }

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
            username = username
        )

        insertToken(geotabAuthState)
        // Reschedule the worker after successful login
        rescheduleTokenRefreshWorker(context, geotabAuthState.username)
    }

    private suspend fun getAuthState(username: String) {
        try {
            val tokensList = getAllTokens()
            val geotabAuthState = getCredentialsFromUsername(username, tokensList) ?: run {
                Logger.shared.error(
                    TAG,
                    "No auth state found for user"
                )

                // Since we couldn't find a token for the user, ensure authState is null
                authState = null
                return
            }

            authState = AuthState.jsonDeserialize(geotabAuthState.authState)
        } catch (e: Exception) {
            Logger.shared.error(
                TAG,
                "Error fetching auth state: ${e.message ?: "Unknown error"}"
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

            tokensList.removeIf { it.username == username }

            val geotabAuthStateChars = JsonUtil.toJson(tokensList).toCharArray()
            insertOrUpdate(AUTH_TOKENS, geotabAuthStateChars)
            cancelScheduleNextRefreshToken(context, username)
        } catch (e: Exception) {
            Logger.shared.error(
                TAG,
                "Error deleting auth token: ${e.message ?: "Unknown error"}"
            )
        }
    }

    private fun getCredentialsFromUsername(username: String, tokensList: MutableList<GeotabAuthState>): GeotabAuthState? {
        return tokensList.firstOrNull { it.username == username }
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
                    "AuthUtil.getAllTokens",
                    "Error parsing auth tokens JSON: ${e.message ?: "Unknown error"}"
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

    private suspend fun launchLogoutUser() {
        val config = authState?.authorizationServiceConfiguration
        val redirectUri = authState?.lastAuthorizationResponse?.request?.redirectUri
        val idToken = authState?.idToken

        if (config == null || redirectUri == null || idToken == null) {
            authState = null
            Logger.shared.error(TAG, "No valid configuration, redirect URI, or ID token found in logout flow")
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

    internal fun handleLogoutResponse() {
        try {
            authState = null

            logoutCallback?.let {
                val message = "Successfully logged out"
                Logger.shared.info(TAG, message)
                it(Success(jsonUtil.toJson(message)))
            }
        } catch (ex: Exception) {
            sendErrorMessage(
                errorMessage = ex.message ?: "Logout failed",
                callback = logoutCallback
            )
        }
    }

    /**
     * Revokes the current tokens by making a manual POST request to the revocation endpoint
     * found in the discovery document.
     *
     * @param tag Current tag for logging purposes.
     */
    private fun revokeToken() {
        val discoveryDoc = authState?.authorizationServiceConfiguration?.discoveryDoc
        val revocationEndpoint = try {
            discoveryDoc?.docJson?.getString("revocation_endpoint")
        } catch (e: Exception) {
            Logger.shared.error(TAG, "No 'revocation_endpoint' found in discovery document.", e)
            null
        }

        if (revocationEndpoint.isNullOrEmpty()) {
            Logger.shared.error(TAG, "Authorization server configuration does not support token revocation.")
            return
        }

        val tokenToRevoke = authState?.refreshToken ?: authState?.accessToken
        if (tokenToRevoke == null) {
            Logger.shared.debug(TAG, "No valid token available to revoke.")
            return
        }

        var connection: HttpURLConnection? = null
        try {
            val revocationUrl = URL(revocationEndpoint)
            connection = revocationUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val clientId = authState?.lastAuthorizationResponse?.request?.clientId
            if (clientId.isNullOrEmpty()) {
                Logger.shared.error(TAG, "Client ID is missing, cannot perform revocation.")
                return
            }

            connection.doOutput = true
            val requestBody = "token=$tokenToRevoke&client_id=$clientId"
            val writer = BufferedWriter(OutputStreamWriter(connection.outputStream, "UTF-8"))
            writer.write(requestBody)
            writer.flush()
            writer.close()

            Logger.shared.info(TAG, "Token revocation response code: ${connection.responseCode}")
        } catch (e: IOException) {
            Logger.shared.error(TAG, "Network error during token revocation.", e)
        } catch (e: Exception) {
            Logger.shared.error(TAG, "An unexpected error occurred during token revocation.", e)
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
                LoginModule.Companion.TAG,
                "Error starting token refresh: ${e.message}"
            )
        }
    }

    fun dispose(context: Context) {
        TokenRefreshWorker.cancelAllTokenRefreshWork(context)
        authService?.dispose()
        authService = null
    }
}
