package com.geotab.mobile.sdk.module.app

interface BackgroundModeAdapter {
    fun startMonitoringBackground(onBackgroundChange: (result: BackgroundMode) -> Unit)
    fun stopMonitoringBackground()
}

data class BackgroundMode(val isBackground: Boolean)
