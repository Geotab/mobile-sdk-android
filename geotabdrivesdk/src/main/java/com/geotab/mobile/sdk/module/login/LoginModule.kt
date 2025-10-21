package com.geotab.mobile.sdk.module.login

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.work.WorkManager
import com.geotab.mobile.sdk.logging.InternalAppLogging
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
import net.openid.appauth.AuthorizationService

@Parcelize
data class GeotabAuthState(
    val authState: String,
    val username: String
) : Parcelable

class LoginModule(
    @Transient private val authUtil: AuthUtil
) : Module(MODULE_NAME) {
    private var isAuthServiceDisposed = false

    @Transient
    lateinit var context: Context

    companion object {
        const val MODULE_NAME = "login"
        const val TAG = "loginModule"
        const val LOGIN_SCHEME_ARGUMENT_ERROR_MESSAGE = "Login redirect scheme not found in resources. Please ensure the string resource is defined with the name [REPLACE]"
    }

    init {
        functions.add(StartFunction(module = this))
        functions.add(GetAuthTokenFunction(module = this))
    }

    fun initValues(activity: ComponentActivity) {
        context = activity.applicationContext
        authUtil.authService = AuthorizationService(context)

        authUtil.activityResultLauncherFunction(
            activityForResult = activity
        )
    }

    fun login(
        clientId: String,
        discoveryUri: Uri,
        loginHint: String,
        redirectScheme: Uri,
        loginFunctionCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        if (isAuthServiceDisposed) {
            authUtil.authService = AuthorizationService(context)
        }

        authUtil.login(
            clientId = clientId,
            discoveryUri = discoveryUri,
            loginHint = loginHint,
            redirectScheme = redirectScheme,
            loginCallback = loginFunctionCallback
        ).also {
            isAuthServiceDisposed = true
        }
    }

    fun dispose() {
        TokenRefreshWorker.cancelAllTokenRefreshWork(context)
        authUtil.authService?.dispose()
    }

    suspend fun startTokenRefresh() {
        val workManager = WorkManager.getInstance(context)
        try {
            // Get all stored user tokens
            val allTokens = authUtil.getAllTokens()
            // Iterate and cancel existing work, then schedule new work
            allTokens.forEach { geotabAuthState ->
                val workName = TokenRefreshWorker.getUniqueWorkName(geotabAuthState.username)
                workManager.cancelUniqueWork(workName)
                authUtil.getValidAccessToken(context, geotabAuthState.username, forceRefresh = true)
            }
        } catch (e: Exception) {
            InternalAppLogging.appLogger?.error(
                TAG,
                "Error starting token refresh: ${e.message}"
            )
        }
    }

    fun handleAuthToken(username: String): AuthToken? {
        val authToken = runBlocking {
            val token = authUtil.getValidAccessToken(context, username)
            token
        }
        return authToken
    }
}
