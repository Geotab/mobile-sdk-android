package com.geotab.mobile.sdk.module.dutyStatusLog

import com.geotab.mobile.sdk.module.Module

class DutyStatusLogModule : Module(MODULE_NAME) {
    companion object {
        const val MODULE_NAME = "dutyStatusLog"
    }

    init {
        functions.add(GetDutyStatusLogFunction(module = this))
        functions.add(GetCurrentDrivingLogFunction(module = this))
    }
}
