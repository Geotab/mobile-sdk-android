package com.geotab.mobile.sdk.module.login

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import com.geotab.mobile.sdk.logging.Logger
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.auth.AuthToken
import com.geotab.mobile.sdk.module.auth.AuthUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.openid.appauth.AuthorizationService

class LoginModule(
    @Transient private val authUtil: AuthUtil
) : Module(MODULE_NAME) {
    private var isAuthServiceDisposed = false

    @Transient
    lateinit var context: Context

    internal val scope = CoroutineScope(Dispatchers.IO)

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

    /**
     * Perform login for a user.
     *
     * @return AuthToken on success
     * @throws Exception on failure
     */
    suspend fun login(
        clientId: String,
        discoveryUri: Uri,
        loginHint: String,
        redirectScheme: Uri
    ): AuthToken {
        Logger.shared.info(TAG, "login function was called")
        if (isAuthServiceDisposed) {
            authUtil.authService = AuthorizationService(context)
        }

        return try {
            authUtil.login(
                context = context,
                clientId = clientId,
                discoveryUri = discoveryUri,
                username = loginHint,
                redirectScheme = redirectScheme,
                comingFromLoginModule = true
            ).also {
                isAuthServiceDisposed = true
            }
        } catch (e: Exception) {
            isAuthServiceDisposed = true
            throw e
        }
    }
}
