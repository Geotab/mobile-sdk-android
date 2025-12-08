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
     * Matches iOS AuthModule.login() - pure suspend function with no callbacks.
     *
     * @return AuthToken on success
     * @throws Exception on failure
     */
    suspend fun login(
        clientId: String,
        discoveryUri: Uri,
        username: String,
        redirectScheme: Uri
    ): AuthToken {
        if (isAuthServiceDisposed) {
            authUtil.authService = AuthorizationService(context)
        }

        return try {
            authUtil.login(
                clientId = clientId,
                discoveryUri = discoveryUri,
                username = username,
                redirectScheme = redirectScheme
            ).also {
                isAuthServiceDisposed = true
            }
        } catch (e: Exception) {
            isAuthServiceDisposed = true
            Logger.shared.error(TAG, "Login failed: ${e.message}", e)
            throw e
        }
    }

    suspend fun handleAuthToken(username: String): AuthToken? =
        try {
            authUtil.getValidAccessToken(context, username)
        } catch (e: Exception) {
            Logger.shared.error(TAG, "Failed to get valid access token for $username: ${e.message}", e)
            null
        }
}
