package com.geotab.mobile.sdk.module.motion

import android.content.Context
import android.widget.Toast
import com.geotab.mobile.sdk.models.ModuleEvent
import com.geotab.mobile.sdk.models.enums.MotionEnum
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.permission.Permission
import com.geotab.mobile.sdk.permission.PermissionDelegate
import com.geotab.mobile.sdk.permission.PermissionHelper

class MotionActivityModule(
    private val context: Context,
    permissionDelegate: PermissionDelegate,
    private val adapter: MotionActivityAdapterDefault = MotionActivityAdapterDefault(context),
    private val push: (ModuleEvent) -> Unit,
    override val name: String = "motion"
) : Module(name) {
    var isPermissionGranted = false
        private set

    private val permissionHelper: PermissionHelper by lazy {
        PermissionHelper(context, permissionDelegate)
    }

    init {
        functions.addAll(
            arrayOf(
                StartMotionActivityFunction(
                    context = context,
                    module = this
                ),
                StopMotionActivityFunction(
                    context = context,
                    module = this
                )
            )
        )
    }

    companion object {
        const val ERROR_MOTION_ACTIVITY = "Error in getting motion activity. "
        const val ERROR_ACTIVATING_MOTION_ACTIVITY = "Motion Activity Not Available"
        const val ERROR_ACTIVATING_MOTION_ACTIVITY_PERMISSION =
            "Motion tracking permission not authorized"
    }

    fun startMonitoringMotionActivity(callback: (Boolean, Boolean) -> Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            permissionHelper.checkPermission(arrayOf(Permission.ACTIVITY_RECOGNITION)) { hasPermission ->
                if (hasPermission) {
                    permissionGrantedAndStartMonitoring(callback)
                } else {
                    isPermissionGranted = false
                    Toast.makeText(
                        context,
                        ERROR_ACTIVATING_MOTION_ACTIVITY_PERMISSION,
                        Toast.LENGTH_LONG
                    ).show()

                    // First false means the user did not grant permission. Second false
                    // means the service was not started
                    callback(false, false)
                }
            }
        } else {
            permissionGrantedAndStartMonitoring(callback)
        }
    }

    fun stopMonitoringMotionActivity() {
        adapter.stopMonitoringMotionActivity()
    }

    private fun permissionGrantedAndStartMonitoring(callback: (Boolean, Boolean) -> Unit) {
        isPermissionGranted = true
        adapter.startMonitoringMotionActivity(
            onMotionActivityChange = {
                onMotionActivityChange(it)
            },
            startCallback = callback
        )
    }

    private fun onMotionActivityChange(motionActivityEnum: MotionEnum) {
        push(
            ModuleEvent(
                "geotab.motion",
                "{detail: ${motionActivityEnum.motionId}}"
            )
        )
    }
}
