package com.geotab.mobile.sdk.module.auth

import androidx.annotation.Keep
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.util.JsonUtil
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.lang.reflect.Type

@Keep
data class GetAuthTokenArgument(
    val username: String
)

class GetTokenFunction(
    override val module: AuthModule,
    override val name: String = "getToken"
) : ModuleFunction, BaseFunction<GetAuthTokenArgument>() {
    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        val arguments = transformOrInvalidate(jsonString, jsCallback) ?: return

        @Suppress("SENSELESS_COMPARISON")
        if (arguments.username.isNullOrBlank()) {
            jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR, "Username is required")))
            return
        }

        module.scope.launch {
            try {
                val authToken = module.handleAuthToken(arguments.username)
                if (authToken != null) {
                    jsCallback(Success(JsonUtil.toJson(authToken)))
                } else {
                    val error = AuthError.NoAccessTokenFoundError(arguments.username)
                    jsCallback(Failure(error))
                }
            } catch (e: Exception) {
                module.handleFunctionException(e, "Get token", jsCallback)
            }
        }
    }

    override fun getType(): Type {
        return object : TypeToken<GetAuthTokenArgument>() {}.type
    }
}
