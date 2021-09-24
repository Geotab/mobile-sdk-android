package com.geotab.mobile.sdk.module.screen

import android.app.Activity
import android.view.WindowManager
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success

class KeepAwakeFunction(private val activity: Activity, override val module: ScreenModule) : ModuleFunction {

    override val name: String = "keepAwake"
    /**
     * transformOrInvalidate is not used here since default interpretation of JSON booleans
     * is anything other than "true" is considered to be false, and we want to return
     * a failure when the module is called with invalid arguments
     */
    override fun handleJavascriptCall(jsonString: String?, jsCallback: (Result<Success<String>, Failure>) -> Unit) {
        when (jsonString) {
            "true" -> {
                activity.runOnUiThread {
                    activity.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                jsCallback(Success(jsonString))
            }
            "false" -> {
                activity.runOnUiThread {
                    activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                jsCallback(Success(jsonString))
            }
            else -> jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
        }
    }
}
