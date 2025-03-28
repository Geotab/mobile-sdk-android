package com.geotab.mobile.sdk.module.dutyStatusLog

import android.content.Context
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseCallbackFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success

class GetCurrentDrivingLogFunction(
    override val name: String = "getCurrentDrivingLog",
    override val module: DutyStatusLogModule
) : ModuleFunction,
    BaseCallbackFunction(name) {
    var userName = ""

    companion object {
        const val templateFileName = "ModuleFunction.GetCurrentDrivingLogFunction.Api.js"
    }

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        val response = this.transformOrInvalidate(jsonString, jsCallback) ?: return
        val sdkCallback = this.getSdkCallbackOrInvalidate(response, jsCallback) ?: return

        if (!response.result.isNullOrEmpty()) {
            sdkCallback(Success(response.result))
        } else {
            val errorMsg = "No current driving log returned"

            jsCallback(Failure(Error(GeotabDriveError.JS_ISSUED_ERROR, errorMsg)))
            sdkCallback(Failure(Error(GeotabDriveError.JS_ISSUED_ERROR, errorMsg)))
            callbacks.remove(response.callerId)
            return
        }

        callbacks.remove(response.callerId)
        jsCallback(Success("undefined"))
    }

    override fun getJavascript(context: Context, callerId: String): String {
        val scriptParameter: HashMap<String, Any> =
            hashMapOf("moduleName" to module.name, "functionName" to name, "userName" to userName)
        scriptParameter.putAll(hashMapOf("callerId" to callerId))
        return module.getScriptFromTemplate(
            context,
            templateFileName, scriptParameter
        )
    }
}
