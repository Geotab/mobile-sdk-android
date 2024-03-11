package com.geotab.mobile.sdk.module.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.geotab.mobile.sdk.util.regReceiver

class BatteryStatusAdapterDefault(private val context: Context) : BatteryStatusAdapter, BroadcastReceiver() {
    private var delegate: ((result: BatteryStatus) -> Unit)? = null

    override fun startMonitoringBatteryStatus(onBatteryStatusChange: (result: BatteryStatus) -> Unit) {
        this.delegate = onBatteryStatusChange
        IntentFilter(Intent.ACTION_BATTERY_CHANGED).let {
            context.regReceiver(broadcastReceiver = this, intentFilter = it, exported = true)
        }
    }

    override fun stopMonitoringBatteryStatus() {
        context.unregisterReceiver(this)
    }

    private fun parseBatteryStatus(batteryStatus: Intent): BatteryStatus {
        val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val batteryLevel = batteryStatus.let {
            val level: Int = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            when (level == -1 || scale < 1) {
                true -> -1
                false -> level * 100 / scale
            }
        }

        return BatteryStatus(isCharging, batteryLevel)
    }

    override fun onReceive(context: Context, intent: Intent) {
        delegate?.let { it(parseBatteryStatus(intent)) }
    }
}
