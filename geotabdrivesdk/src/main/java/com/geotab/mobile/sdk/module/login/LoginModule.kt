package com.geotab.mobile.sdk.module.login

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.util.AuthUtil
import kotlinx.parcelize.Parcelize
import net.openid.appauth.AuthorizationService

@Parcelize
data class GeotabAuthState(
    val accessToken: String,
    val idToken: String,
    val refreshToken: String
) : Parcelable

class LoginModule(
    val context: Context
) : Module(MODULE_NAME) {
    private val authUtil: AuthUtil by lazy {
        AuthUtil()
    }

    private lateinit var authResultLauncher: ActivityResultLauncher<Intent>
    private var isAuthServiceDisposed = false

    companion object {
        const val MODULE_NAME = "login"
        const val TAG = "loginModule"
        const val LOGIN_SCHEME_ARGUMENT_ERROR_MESSAGE = "Login redirect scheme not found in resources. Please ensure the string resource is defined with the name [REPLACE]"
    }

    init {
        functions.add(
            StartFunction(
                module = this
            )
        )
    }

    fun initValues(fragment: Fragment) {
        authUtil.authService = AuthorizationService(context)

        authResultLauncher = authUtil.activityResultLauncherFunction(
            fragmentForResult = fragment,
            tag = TAG
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
            loginCallback = loginFunctionCallback,
            tag = TAG,
            authResultLauncher = authResultLauncher
        ).also {
            isAuthServiceDisposed = true
        }
    }

    fun disposeAuthService() {
        authUtil.authService?.dispose()
    }
}
