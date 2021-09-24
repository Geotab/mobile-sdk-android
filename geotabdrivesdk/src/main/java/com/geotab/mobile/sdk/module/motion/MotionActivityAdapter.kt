package com.geotab.mobile.sdk.module.motion

import com.geotab.mobile.sdk.models.enums.MotionEnum

interface MotionActivityAdapter {
    fun startMonitoringMotionActivity(
        onMotionActivityChange: (result: MotionEnum) -> Unit,
        startCallback: (Boolean, Boolean) -> Unit
    )

    fun stopMonitoringMotionActivity()
}
