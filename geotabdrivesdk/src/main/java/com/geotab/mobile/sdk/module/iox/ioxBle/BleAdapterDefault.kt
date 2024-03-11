package com.geotab.mobile.sdk.module.iox.ioxBle

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import com.geotab.mobile.sdk.util.regReceiver
import java.util.UUID

class BleAdapterDefault(val context: Context) : BleAdapter, BroadcastReceiver() {
    companion object {
        const val TAG = "BLE_ADAPTER"
    }

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter? =
        bluetoothManager.adapter

    private var gattServer: BluetoothGattServer? = null

    override val isBleSupported: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    override val isBleAdvertisingSupported: Boolean
        get() = bluetoothAdapter?.bluetoothLeAdvertiser != null

    override val isBleEnabled: Boolean
        get() = bluetoothManager.adapter != null && bluetoothManager.adapter.isEnabled

    private var bleStateOffCallback: (() -> Unit)? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "On BLE state change")
        when (intent?.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state =
                    intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
                if (state == BluetoothAdapter.STATE_OFF) {
                    Log.d(TAG, "BLE adapter is off")
                    bleStateOffCallback?.invoke()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun startAdvertise(serviceId: String?, callback: AdvertiseCallback) {
        val advertiseSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .setTimeout(0)
            .build()
        val advertiseData = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(UUID.fromString(serviceId)))
            .build()

        bluetoothAdapter?.bluetoothLeAdvertiser?.startAdvertising(
            advertiseSettings,
            advertiseData,
            callback
        )
    }

    @SuppressLint("MissingPermission")
    override fun stopAdvertise(advertisingCallback: AdvertiseCallback) {
        bluetoothAdapter?.bluetoothLeAdvertiser?.stopAdvertising(advertisingCallback)
    }

    @SuppressLint("MissingPermission")
    override fun startServer(
        service: BluetoothGattService,
        bluetoothGattServerCallback: BluetoothGattServerCallback
    ) {
        gattServer = bluetoothManager.openGattServer(context, bluetoothGattServerCallback).apply {
            addService(service)
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopServer() {
        gattServer?.apply {
            clearServices()
            close()
        }
    }

    @SuppressLint("MissingPermission")
    override fun connect(bluetoothDevice: BluetoothDevice): Boolean {
        var connect = false
        gattServer?.apply {
            connect = connect(bluetoothDevice, false)
        }
        return connect
    }

    @SuppressLint("MissingPermission")
    override fun cancelConnection(bluetoothDevice: BluetoothDevice) {
        gattServer?.apply {
            cancelConnection(bluetoothDevice)
        }
    }

    @SuppressLint("MissingPermission")
    override fun notifyCharacteristicUpdate(
        bluetoothDevice: BluetoothDevice,
        bluetoothGattCharacteristic: BluetoothGattCharacteristic
    ) {
        gattServer?.apply {
            notifyCharacteristicChanged(bluetoothDevice, bluetoothGattCharacteristic, false)
        }
    }

    @SuppressLint("MissingPermission")
    override fun sendResponse(bluetoothDevice: BluetoothDevice, requestId: Int) {
        Log.d(TAG, "Sending response to the remote device for the read or write request")
        gattServer?.apply {
            sendResponse(bluetoothDevice, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
        }
    }

    override fun registerBleStateOffCallback(callback: (() -> Unit)) {
        if (bleStateOffCallback == null) {
            val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            context.regReceiver(broadcastReceiver = this, intentFilter = filter, exported = true)
            bleStateOffCallback = callback
        }
    }

    override fun unregisterBleStateOffCallback() {
        bleStateOffCallback?.let {
            bleStateOffCallback = null
            context.unregisterReceiver(this)
        }
    }
}
