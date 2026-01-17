package com.geotab.mobile.sdk.module.login

import android.annotation.SuppressLint
import androidx.annotation.Keep
import androidx.core.net.toUri
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.R
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.login.LoginModule.Companion.LOGIN_SCHEME_ARGUMENT_ERROR_MESSAGE
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.lang.reflect.Type

@Keep
data class LoginArgument(
    val clientId: String,
    val discoveryUri: String,
    val loginHint: String
)

class StartFunction(
    override val module: LoginModule,
    override val name: String = "start"
) : ModuleFunction, BaseFunction<LoginArgument>() {
    @SuppressLint("DiscouragedApi") // Suppressing lint warning for using getIdentifier
    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        val arguments = transformOrInvalidate(jsonString, jsCallback) ?: return

        @Suppress("SENSELESS_COMPARISON")
        if (
            arguments.clientId.isNullOrBlank() ||
            arguments.discoveryUri.isNullOrBlank()
        ) {
            jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
            return
        }

        // Trim loginHint (returns empty string if null) and validate it's not empty
        @Suppress("UNNECESSARY_SAFE_CALL")
        val trimmedLoginHint = arguments.loginHint?.trim() ?: ""

        if (trimmedLoginHint.isEmpty()) {
            jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR, "LoginHint is required")))
            return
        }

        val discoveryUri = arguments.discoveryUri.toUri().normalizeScheme()

        if (discoveryUri.scheme != "https") {
            jsCallback(
                Failure(
                    Error(
                        GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR,
                        "Insecure Discovery URI. HTTPS is required"
                    )
                )
            )
            return
        }

        val loginRedirectSchemeString = module.context.resources.getString(R.string.geotab_login_redirect_scheme)

        val resourceId = module.context.resources.getIdentifier(
            loginRedirectSchemeString,
            "string",
            module.context.packageName
        )

        if (resourceId == 0) {
            jsCallback(
                Failure(
                    Error(
                        GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR,
                        LOGIN_SCHEME_ARGUMENT_ERROR_MESSAGE.replace("[REPLACE]", loginRedirectSchemeString)
                    )
                )
            )
            return
        }

        module.scope.launch {
            try {
                val authToken = module.login(
                    clientId = arguments.clientId,
                    discoveryUri = discoveryUri,
                    loginHint = trimmedLoginHint,
                    redirectScheme = module.context.resources.getString(resourceId).toUri()
                )
                jsCallback(Success(com.geotab.mobile.sdk.util.JsonUtil.toJson(authToken)))
            } catch (e: Exception) {
                val errorMessage = if (e is com.geotab.mobile.sdk.module.auth.AuthError) e.errorDescription else e.message ?: "Login failed"
                jsCallback(Failure(Error(GeotabDriveError.AUTH_FAILED_ERROR, errorMessage)))
            }
        }
    }

    override fun getType(): Type {
        return object : TypeToken<LoginArgument>() {}.type
    }
}
