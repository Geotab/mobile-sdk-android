package com.geotab.mobile.sdk.module.user

import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.DriverActionNecessaryArgument
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class DriverActionNecessaryFunction(override val name: String = "driverActionNecessary", override val module: UserModule) : ModuleFunction,
    BaseFunction<DriverActionNecessaryArgument>() {

    override fun handleJavascriptCall(jsonString: String?, jsCallback: (Result<Success<String>, Failure>) -> Unit) {
        val response = this.transformOrInvalidate(jsonString, jsCallback) ?: return

        if (response.driverActionType.isNotEmpty()) {
            module.driverActionNecessaryCallback(response)
            jsCallback(Success("undefined"))
        } else {
            jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
        }
    }

    override fun getType(): Type {
        return object : TypeToken<DriverActionNecessaryArgument>() {}.type
    }
}
