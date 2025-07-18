package com.geotab.mobile.sdk.module.device

import android.content.Context
import android.content.SharedPreferences
import com.geotab.mobile.sdk.models.Device
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.util.UserAgentUtil

class DeviceModule(
    context: Context,
    preferences: SharedPreferences,
    private val userAgentUtil: UserAgentUtil
) : Module(MODULE_NAME) {

    private val device: Device = Device(context, preferences)

    companion object {
        const val MODULE_NAME = "device"
    }

    override fun scripts(context: Context): String {

        var scripts = super.scripts(context)
        // create hashmap to populate values into script
        val deviceModuleVal: HashMap<String, Any> = hashMapOf(
            "geotabModules" to geotabModules,
            "moduleName" to name,
            "devicePlatform" to device.platform,
            "deviceManufacturer" to device.manufacturer,
            "version" to userAgentUtil.version,
            "appId" to device.appId,
            "appName" to userAgentUtil.appName,
            "sdkVersion" to device.sdkVersion,
            "deviceModel" to device.model,
            "deviceUuid" to device.uuid
        )

        scripts += getScriptFromTemplate(context, "DeviceModule.Script.js", deviceModuleVal)
        return scripts
    }
}
