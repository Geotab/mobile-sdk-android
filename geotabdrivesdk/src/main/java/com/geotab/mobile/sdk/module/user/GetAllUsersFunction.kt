package com.geotab.mobile.sdk.module.user

import android.content.Context
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseCallbackFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success

open class GetAllUsersFunction(
    override val name: String = "getAll",
    override val module: UserModule
) : ModuleFunction,
    BaseCallbackFunction(name) {
    var includeAllUsers = true
    companion object {
        const val templateFileName = "ModuleFunction.GetAllUsersFunction.Api.js"
    }

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        val response = this.transformOrInvalidate(jsonString, jsCallback) ?: return
        val sdkCallback = this.getSdkCallbackOrInvalidate(response, jsCallback) ?: return

        if (!response.result.isNullOrEmpty()) {
            sdkCallback(Success(response.result))
            jsCallback(Success("undefined"))
        } else {
            sdkCallback(Failure(Error(GeotabDriveError.JS_ISSUED_ERROR, "No users returned")))
            jsCallback(Failure(Error(GeotabDriveError.JS_ISSUED_ERROR, "No users returned")))
        }
        callbacks.remove(response.callerId)
    }

    override fun getJavascript(context: Context, callerId: String): String {
        val scriptParameter: HashMap<String, Any> =
            hashMapOf("moduleName" to module.name, "functionName" to name, "getAllUsers" to includeAllUsers)
        scriptParameter.putAll(hashMapOf("callerId" to callerId))
        return module.getScriptFromTemplate(context, templateFileName, scriptParameter)
    }
}
