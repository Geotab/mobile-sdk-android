package com.geotab.mobile.sdk.module.state

import com.geotab.mobile.sdk.module.Module

class StateModule : Module(MODULE_NAME) {
    init {
        functions.add(DeviceFunction(module = this))
    }

    companion object {
        const val MODULE_NAME = "state"
    }
}
