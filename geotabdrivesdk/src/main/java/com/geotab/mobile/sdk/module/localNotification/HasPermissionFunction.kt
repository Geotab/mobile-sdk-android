package com.geotab.mobile.sdk.module.localNotification

import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success

class HasPermissionFunction(override val name: String = "hasPermission", override val module: LocalNotificationModule) :
    ModuleFunction {
    override fun handleJavascriptCall(jsonString: String?, jsCallback: (Result<Success<String>, Failure>) -> Unit) {
        jsCallback(Success(module.checkPermission().toString()))
    }
}
