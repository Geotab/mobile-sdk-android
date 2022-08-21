package com.geotab.mobile.sdk.module.user

import android.content.Context
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseCallbackFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success

class GetViolationsFunction(
    override val name: String = "getViolations",
    override val module: UserModule
) : ModuleFunction,
    BaseCallbackFunction(name) {
    var userName = ""

    companion object {
        const val templateFileName = "ModuleFunction.GetViolationsFunction.Api.js"
    }

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        println("response for violation:" + jsonString)
        val response = this.transformOrInvalidate(jsonString, jsCallback) ?: return
        val sdkCallback = this.getSdkCallbackOrInvalidate(response, jsCallback) ?: return

        if (!response.result.isNullOrBlank()) {
            sdkCallback(Success(response.result))
            jsCallback(Success("undefined"))
        } else {
            sdkCallback(
                Failure(
                    Error(
                        GeotabDriveError.JS_ISSUED_ERROR,
                        "No DutyStatusViolations returned"
                    )
                )
            )
            jsCallback(
                Failure(
                    Error(
                        GeotabDriveError.JS_ISSUED_ERROR,
                        "No DutyStatusViolations returned"
                    )
                )
            )
        }

        callbacks.remove(response.callerId)
    }

    override fun getJavascript(context: Context, callerId: String): String {
        val scriptParameter: HashMap<String, Any> =
            hashMapOf("moduleName" to module.name, "functionName" to name, "userName" to userName)
        scriptParameter.putAll(hashMapOf("callerId" to callerId))
        return module.getScriptFromTemplate(context, templateFileName, scriptParameter)
    }
}
