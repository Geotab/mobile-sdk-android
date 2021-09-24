package com.geotab.mobile.sdk.module.state

import com.geotab.mobile.sdk.module.Module

class StateModule(override val name: String = "state") : Module(name) {
    init {
        functions.add(DeviceFunction(module = this))
    }
}
