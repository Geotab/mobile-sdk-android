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

        // Trim username (returns empty string if null) and validate it's not empty
        @Suppress("UNNECESSARY_SAFE_CALL")
        val trimmedUsername = arguments.username?.trim() ?: ""

        if (trimmedUsername.isEmpty()) {
            jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR, AuthModule.USERNAME_REQUIRED_ERROR_MESSAGE)))
            return
        }

        module.scope.launch {
            try {
                val authToken = module.handleAuthToken(trimmedUsername)
                if (authToken != null) {
                    jsCallback(Success(JsonUtil.toJson(authToken)))
                } else {
                    val error = AuthError.NoAccessTokenFoundError(trimmedUsername)
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
