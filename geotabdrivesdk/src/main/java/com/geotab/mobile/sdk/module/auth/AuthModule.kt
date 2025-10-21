package com.geotab.mobile.sdk.module.auth

import android.content.Context
import androidx.activity.ComponentActivity
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.login.AuthUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationService

class AuthModule(
    @Transient private val authUtil: AuthUtil
) : Module(MODULE_NAME) {

    @Transient
    lateinit var context: Context

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val MODULE_NAME = "auth"
        const val TAG = "authModule"
    }

    init {
        functions.add(LogoutFunction(module = this))
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

    fun dispose() {
        authUtil.authService?.dispose()
        authUtil.authService = null
    }
}
