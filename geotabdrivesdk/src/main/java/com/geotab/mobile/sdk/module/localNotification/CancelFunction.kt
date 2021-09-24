package com.geotab.mobile.sdk.module.localNotification

import android.content.Context
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class CancelFunction(
    override val name: String = "cancel",
    override val module: LocalNotificationModule
) : ModuleFunction, BaseFunction<Int>() {
    companion object {
        const val templateFileName = "ModuleFunction.LocalNotification.Cancel.Script.js"
    }

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        val id = transformOrInvalidate(jsonString, jsCallback) ?: return
        module.cancelNotification(id)?.let { jsCallback(Success(it)) } ?: run {
            jsCallback(Failure(Error(GeotabDriveError.NOTIFICATION_NOT_FOUND)))
        }
    }

    override fun getScriptData(): HashMap<String, Any> {
        var offName = "off"
        val offFunction = module.findFunction(name = "off")
        if (offFunction is OffFunction) {
            offName = offFunction.name
        }

        return hashMapOf(
            "geotabModules" to Module.geotabModules,
            "moduleName" to module.name,
            "geotabNativeCallbacks" to Module.geotabNativeCallbacks,
            "callbackPrefix" to Module.callbackPrefix,
            "off" to offName,
            "functionName" to name,
            "interfaceName" to Module.interfaceName
        )
    }

    override fun scripts(context: Context): String {
        return module.getScriptFromTemplate(context, templateFileName, getScriptData())
    }

    override fun getType(): Type {
        return object : TypeToken<Int>() {}.type
    }
}
