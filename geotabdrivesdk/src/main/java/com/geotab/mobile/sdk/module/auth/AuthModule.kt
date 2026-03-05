package com.geotab.mobile.sdk.module.auth

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    fun logout(
        username: String,
        logoutFunctionCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        scope.launch {
            authUtil.logout(
                context = context,
                username = username,
                logoutCallbackFromModule = logoutFunctionCallback
            )
        }
    }

    fun login(
        clientId: String,
        discoveryUri: Uri,
        username: String,
        redirectScheme: Uri,
        loginFunctionCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        if (isAuthServiceDisposed) {
            authUtil.authService = AuthorizationService(context)
        }

        authUtil.login(
            clientId = clientId,
            discoveryUri = discoveryUri,
            username = username,
            redirectScheme = redirectScheme,
            loginCallback = loginFunctionCallback
        ).also {
            isAuthServiceDisposed = true
        }
    }

    suspend fun handleAuthToken(username: String): AuthToken? =
        authUtil.getValidAccessToken(context, username)
}
