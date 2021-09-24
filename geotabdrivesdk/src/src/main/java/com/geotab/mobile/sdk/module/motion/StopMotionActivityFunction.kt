package com.geotab.mobile.sdk.module.motion

import android.content.Context
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success

class StopMotionActivityFunction(
    val context: Context,
    override val name: String = "stopMonitoringMotionActivity",
    override val module: MotionActivityModule
) : ModuleFunction {
    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        try {
            module.stopMonitoringMotionActivity()
        } finally {
            jsCallback(Success("undefined"))
            return
        }
    }
}
