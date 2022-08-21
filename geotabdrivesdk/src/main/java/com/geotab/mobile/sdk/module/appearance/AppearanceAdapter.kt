package com.geotab.mobile.sdk.module.appearance

import com.geotab.mobile.sdk.models.enums.AppearanceType

interface AppearanceAdapter {
    fun startMonitoringAppearance(onAppearanceChange: (result: AppearanceType) -> Unit)
    fun stopMonitoringAppearance()
}
