package com.geotab.mobile.sdk.module.user

import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success

class PageNavigationFunction(override val name: String = "pageNavigation", override val module: UserModule) : ModuleFunction {

    override fun handleJavascriptCall(jsonString: String?, jsCallback: (Result<Success<String>, Failure>) -> Unit) {
        jsonString?.let {
            if (it.isNotEmpty()) {
                module.pageNavigationCallback(it)
                jsCallback(Success("undefined"))
                return
            }
        }
        jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
    }
}
