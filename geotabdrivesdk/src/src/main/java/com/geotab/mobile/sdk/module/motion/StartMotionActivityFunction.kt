package com.geotab.mobile.sdk.module.motion

import android.content.Context
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success

class StartMotionActivityFunction(
    val context: Context,
    override val name: String = "startMonitoringMotionActivity",
    override val module: MotionActivityModule
) : ModuleFunction {
    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        try {
            module.startMonitoringMotionActivity { hasPermission, isMotionActivityRegistered ->
                when {
                    !hasPermission -> {
                        jsCallback(
                            Failure(
                                Error(
                                    GeotabDriveError.MODULE_MOTION_ACTIVITY_ERROR,
                                    MotionActivityModule.ERROR_ACTIVATING_MOTION_ACTIVITY_PERMISSION
                                )
                            )
                        )
                    }

                    !isMotionActivityRegistered -> {
                        jsCallback(
                            Failure(
                                Error(
                                    GeotabDriveError.MODULE_MOTION_ACTIVITY_ERROR,
                                    MotionActivityModule.ERROR_ACTIVATING_MOTION_ACTIVITY
                                )
                            )
                        )
                    }

                    else -> {
                        jsCallback(Success("undefined"))
                    }
                }
            }
        } catch (e: Exception) {
            val message = e.message?.let { ": $it" } ?: ""
            jsCallback(
                Failure(
                    Error(
                        GeotabDriveError.MODULE_MOTION_ACTIVITY_ERROR,
                        MotionActivityModule.ERROR_MOTION_ACTIVITY + message
                    )
                )
            )
            return
        }
    }
}
