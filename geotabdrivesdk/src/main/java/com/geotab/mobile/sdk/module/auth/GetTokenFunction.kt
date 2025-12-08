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
                    jsCallback(Failure(Error(GeotabDriveError.AUTH_FAILED_ERROR, "No auth token found for user ${arguments.username}")))
                }
            } catch (e: Exception) {
                com.geotab.mobile.sdk.logging.Logger.shared.error("GetTokenFunction", "Get token failed: ${e.message}", e)
                jsCallback(Failure(Error(GeotabDriveError.AUTH_FAILED_ERROR, e.message ?: "Get token failed")))
            }
        }
    }

    override fun getType(): Type {
        return object : TypeToken<GetAuthTokenArgument>() {}.type
    }
}
