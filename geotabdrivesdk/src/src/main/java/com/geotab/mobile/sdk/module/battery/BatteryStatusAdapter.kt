package com.geotab.mobile.sdk.module.battery

interface BatteryStatusAdapter {
    fun startMonitoringBatteryStatus(onBatteryStatusChange: (result: BatteryStatus) -> Unit)
    fun stopMonitoringBatteryStatus()
}

data class BatteryStatus(val isCharging: Boolean, val level: Int)
