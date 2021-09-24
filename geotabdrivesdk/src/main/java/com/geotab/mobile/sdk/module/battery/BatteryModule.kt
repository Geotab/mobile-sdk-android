package com.geotab.mobile.sdk.module.battery

import android.content.Context
import com.geotab.mobile.sdk.models.ModuleEvent
import com.geotab.mobile.sdk.module.Module

class BatteryModule(
    context: Context,
    private val adapter: BatteryStatusAdapter = BatteryStatusAdapterDefault(context),
    private val push: (ModuleEvent) -> Unit,
    override val name: String = "battery"
) : Module(name) {

    var isCharging: Boolean = false
        private set

    fun startMonitoringBatteryStatus() {
        adapter.startMonitoringBatteryStatus { onBatteryStatusChange(it) }
    }

    fun stopMonitoringBatteryStatus() {
        adapter.stopMonitoringBatteryStatus()
    }

    private fun onBatteryStatusChange(batteryStatus: BatteryStatus) {
        isCharging = batteryStatus.isCharging
        push(
            ModuleEvent(
                "batterystatus",
                "{ detail: { isPlugged: ${batteryStatus.isCharging}, level: ${batteryStatus.level}}}"
            )
        )
    }
}
