package com.geotab.mobile.sdk.module.localNotification

import android.content.Context
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class OffFunction(override val name: String = "off", override val module: LocalNotificationModule) :
    ModuleFunction, BaseFunction<Array<String>>() {

    companion object {
        const val templateFileName = "ModuleFunction.Off.Script.js"
    }

    override fun handleJavascriptCall(jsonString: String?, jsCallback: (Result<Success<String>, Failure>) -> Unit) {
        val data = this.transformOrInvalidate(jsonString, jsCallback) ?: return

        if (LocalNotificationModule.actionHandler == null) {
            return
        }

        LocalNotificationModule.actionIdentifier = data
    }

    override fun getScriptData(): HashMap<String, Any> {
        return hashMapOf(
            "geotabModules" to Module.geotabModules,
            "moduleName" to module.name,
            "functionName" to name,
            "interfaceName" to Module.interfaceName
        )
    }
    override fun scripts(context: Context): String {
        return module.getScriptFromTemplate(context, templateFileName, getScriptData())
    }

    override fun getType(): Type {
        return object : TypeToken<Array<String>>() {}.type
    }
}
