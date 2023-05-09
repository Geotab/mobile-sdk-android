package com.geotab.mobile.sdk.module.screen

import android.app.Activity
import com.geotab.mobile.sdk.module.Module

class ScreenModule(activity: Activity) : Module(MODULE_NAME) {
    init {
        functions.add(KeepAwakeFunction(activity, module = this))
    }

    companion object {
        const val MODULE_NAME = "screen"
    }
}
