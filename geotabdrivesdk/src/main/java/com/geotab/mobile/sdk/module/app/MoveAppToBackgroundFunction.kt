package com.geotab.mobile.sdk.module.app

import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success

class MoveAppToBackgroundFunction(
    val moveAppToBackground: () -> Unit,
    override val name: String = "moveAppToBackground",
    override val module: AppModule
) : ModuleFunction {
    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        moveAppToBackground()
    }
}
