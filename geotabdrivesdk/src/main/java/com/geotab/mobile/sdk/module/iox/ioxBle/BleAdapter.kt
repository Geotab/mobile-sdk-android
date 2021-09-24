package com.geotab.mobile.sdk.module.iox.ioxBle

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.AdvertiseCallback

interface BleAdapter {
    val isBleEnabled: Boolean
    val isBleSupported: Boolean
    val isBleAdvertisingSupported: Boolean

    fun startAdvertise(serviceId: String?, callback: AdvertiseCallback)

    fun stopAdvertise(advertisingCallback: AdvertiseCallback)

    fun startServer(
        service: BluetoothGattService,
        bluetoothGattServerCallback: BluetoothGattServerCallback
    )

    fun sendResponse(bluetoothDevice: BluetoothDevice, requestId: Int)

    fun stopServer()

    fun connect(bluetoothDevice: BluetoothDevice): Boolean

    fun cancelConnection(bluetoothDevice: BluetoothDevice)

    fun notifyCharacteristicUpdate(
        bluetoothDevice: BluetoothDevice,
        bluetoothGattCharacteristic: BluetoothGattCharacteristic
    )

    fun registerBleStateOffCallback(callback: (() -> Unit))

    fun unregisterBleStateOffCallback()
}
