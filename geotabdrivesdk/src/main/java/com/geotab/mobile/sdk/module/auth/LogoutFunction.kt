package com.geotab.mobile.sdk.module.auth

import androidx.annotation.Keep
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.lang.reflect.Type

@Keep
data class LogoutArgument(
    val username: String
)

class LogoutFunction(
    override val module: AuthModule,
    override val name: String = "logout"
) : ModuleFunction, BaseFunction<LogoutArgument>() {
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

        // Normalize to lowercase
        val normalizedUsername = trimmedUsername.lowercase()

        module.scope.launch {
            try {
                module.logout(username = normalizedUsername)
                jsCallback(Success(com.geotab.mobile.sdk.util.JsonUtil.toJson("Logged out successfully")))
            } catch (e: Exception) {
                module.handleFunctionException(e, "Logout", jsCallback)
            }
        }
    }

    override fun getType(): Type {
        return object : TypeToken<LogoutArgument>() {}.type
    }
}
