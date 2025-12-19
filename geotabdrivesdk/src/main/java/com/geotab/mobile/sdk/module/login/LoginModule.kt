package com.geotab.mobile.sdk.module.login

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import com.geotab.mobile.sdk.logging.Logger
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.auth.AuthToken
import com.geotab.mobile.sdk.module.auth.AuthUtil
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthorizationService

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
        Logger.shared.info(TAG, "login function was called")
        if (isAuthServiceDisposed) {
            authUtil.authService = AuthorizationService(context)
        }

        authUtil.login(
            clientId = clientId,
            discoveryUri = discoveryUri,
            username = loginHint,
            redirectScheme = redirectScheme,
            comingFromLoginModule = true,
            loginCallback = loginFunctionCallback
        ).also {
            isAuthServiceDisposed = true
        }
    }

    fun handleAuthToken(username: String): AuthToken? {
        Logger.shared.info(TAG, "handleAuthToken function was called")
        val authToken = runBlocking {
            val token = authUtil.getValidAccessToken(context, username)
            token
        }
        return authToken
    }
}
