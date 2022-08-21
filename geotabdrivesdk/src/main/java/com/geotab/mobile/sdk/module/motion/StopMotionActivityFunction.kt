package com.geotab.mobile.sdk.module.motion

import android.content.Context
import android.util.Log
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.motion.MotionActivityModule.Companion.ERROR_STOP_MOTION_ACTIVITY
import com.geotab.mobile.sdk.module.motion.MotionActivityModule.Companion.TAG

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
        } catch (e: Exception) {
            Log.e(TAG, ERROR_STOP_MOTION_ACTIVITY)
        }
        jsCallback(Success("undefined"))
    }
}
