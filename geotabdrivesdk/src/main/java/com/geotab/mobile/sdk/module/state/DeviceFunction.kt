package com.geotab.mobile.sdk.module.state

import android.content.Context
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseCallbackFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success

class DeviceFunction(override val name: String = "device", override var module: StateModule) :
    ModuleFunction, BaseCallbackFunction(name) {
    companion object {
        const val templateFileName = "ModuleFunction.DeviceState.Api.js"
    }

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        val response = this.transformOrInvalidate(jsonString, jsCallback) ?: return
        val sdkCallback = this.getSdkCallbackOrInvalidate(response, jsCallback) ?: return

        if (response.result.isNullOrBlank()) {
            val errorMsg = "No DeviceState returned"
            sdkCallback(Failure(Error(GeotabDriveError.JS_ISSUED_ERROR, errorMsg)))
            jsCallback(Failure(Error(GeotabDriveError.JS_ISSUED_ERROR, errorMsg)))
            callbacks.remove(response.callerId)
            return
        }

        sdkCallback(Success(response.result))
        callbacks.remove(response.callerId)
        jsCallback(Success("undefined"))
    }

    override fun getJavascript(context: Context, callerId: String): String {
        val scriptParameter: HashMap<String, Any> =
            hashMapOf("moduleName" to module.name, "functionName" to name)
        scriptParameter.putAll(hashMapOf("callerId" to callerId))
        return module.getScriptFromTemplate(context, templateFileName, scriptParameter)
    }
}
