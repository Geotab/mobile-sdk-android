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

        @Suppress("SENSELESS_COMPARISON")
        if (arguments.username.isNullOrBlank()) {
            jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR, "Username is required")))
            return
        }

        module.logout(
            username = arguments.username,
            logoutFunctionCallback = jsCallback
        )
    }

    override fun getType(): Type {
        return object : TypeToken<LogoutArgument>() {}.type
    }
}
