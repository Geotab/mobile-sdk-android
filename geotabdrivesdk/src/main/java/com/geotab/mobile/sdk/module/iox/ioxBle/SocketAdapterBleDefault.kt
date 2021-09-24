package com.geotab.mobile.sdk.module.iox.ioxBle

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.util.Log
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.iox.ACK
import com.geotab.mobile.sdk.module.iox.HANDSHAKE
import com.geotab.mobile.sdk.module.iox.SocketAdapter
import com.geotab.mobile.sdk.module.iox.isGoDeviceData
import java.util.UUID

class SocketAdapterBleDefault internal constructor(private val bleAdapter: BleAdapter) :
    SocketAdapter() {

    constructor(context: Context) : this(BleAdapterDefault(context))

    companion object {
        const val TAG = "SOCKET_BLE"
        const val NOTIFY_CHARACTERISTIC_UUID = "430F2EA3-C765-4051-9134-A341254CFD00"
        const val WRITE_CHARACTERISTIC_UUID = "906EE7E0-D8DB-44F3-AF54-6B0DFCECDF1C"
        const val NOTIFICATION_DESCRIPTOR_ID = "00002902-0000-1000-8000-00805f9b34fb"
        const val BLE_NOT_SUPPORTED = "BLE is not supported for this device"
        const val BLE_ADVERTISE_NOT_SUPPORTED = "BLE Advertising is not supported"
        const val BLE_NOT_ENABLED = "BLE is not enabled or ready to use"
        const val BLE_ALREADY_CONNECTED = "BLE service is already started"
        const val BLE_FAILED_ADVERTISING = "BLE failed to advertise"
        const val BLE_POWERED_OFF = "BLE is power off state"
        const val BLE_DISCONNECTED = "BLE is disconnected"
    }

    internal sealed class State {
        object Idle : State()
        object Opening : State()
        object Advertising : State()
        object Connecting : State()
        object Connected : State()
    }

    internal var state: State = State.Idle
        private set

    private var listener: Listener? = null
    private var bluetoothDevice: BluetoothDevice? = null
    private var partialGoDeviceData: PartialGoDeviceData? = null

    override fun open(listener: Listener, autoConnect: Boolean) {
        this.listener = listener
        try {
            if (state != State.Idle) {
                this.listener?.onOpen(
                    Error(
                        GeotabDriveError.MODULE_BLE_ERROR,
                        BLE_ALREADY_CONNECTED
                    )
                )
                return
            }

            if (!bleAdapter.isBleSupported) {
                this.listener?.onOpen(Error(GeotabDriveError.MODULE_BLE_ERROR, BLE_NOT_SUPPORTED))
                return
            }

            if (!bleAdapter.isBleEnabled) {
                this.listener?.onOpen(Error(GeotabDriveError.MODULE_BLE_ERROR, BLE_NOT_ENABLED))
                return
            }

            if (!bleAdapter.isBleAdvertisingSupported) {
                this.listener?.onOpen(
                    Error(
                        GeotabDriveError.MODULE_BLE_ERROR,
                        BLE_ADVERTISE_NOT_SUPPORTED
                    )
                )
                return
            }

            bleAdapter.registerBleStateOffCallback {
                if (state != State.Idle) {
                    this.listener?.onCloseUnexpectedly(
                        Error(
                            GeotabDriveError.MODULE_BLE_ERROR,
                            BLE_POWERED_OFF
                        )
                    )
                }
                stopBle(advertisingCallback)
            }

            val service = BluetoothGattService(
                UUID.fromString(uuidStr),
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )

            val descriptorID = UUID.fromString(NOTIFICATION_DESCRIPTOR_ID)
            val descriptor = BluetoothGattDescriptor(
                descriptorID,
                BluetoothGattDescriptor.PERMISSION_WRITE or BluetoothGattDescriptor.PERMISSION_READ
            )
            notifyCharacteristic.addDescriptor(descriptor)
            service.addCharacteristic(notifyCharacteristic)
            service.addCharacteristic(writeCharacteristic)

            bleAdapter.startServer(service, bluetoothGattServerCallback)
            state = State.Opening
            bleAdapter.startAdvertise(uuidStr, advertisingCallback)
        } catch (ex: Exception) {
            Log.d(IoxBleModule.TAG, "startBle exception " + ex.stackTrace)
            this.listener?.onOpen(Error(GeotabDriveError.MODULE_BLE_ERROR, ex.message))
        }
    }

    override fun close() {
        stopBle(advertisingCallback)
        this.listener = null
    }

    override fun write(byteArray: ByteArray) {
        notifyCharacteristic.value = byteArray
        bluetoothDevice?.let {
            bleAdapter.notifyCharacteristicUpdate(
                it,
                notifyCharacteristic
            )
        }
    }

    private val notifyCharacteristic = BluetoothGattCharacteristic(
        UUID.fromString(NOTIFY_CHARACTERISTIC_UUID),
        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
        BluetoothGattCharacteristic.PERMISSION_READ
    )

    private val writeCharacteristic = BluetoothGattCharacteristic(
        UUID.fromString(WRITE_CHARACTERISTIC_UUID),
        BluetoothGattCharacteristic.PROPERTY_WRITE,
        BluetoothGattCharacteristic.PERMISSION_WRITE
    )

    private val advertisingCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "Advertising Callback onStartSuccess")
            if (state != State.Opening) return
            state = State.Advertising
        }

        override fun onStartFailure(errorCode: Int) {
            Log.d(TAG, "Advertising Callback onStartFailure")
            if (state != State.Opening) return
            listener?.onOpen(
                Error(
                    GeotabDriveError.MODULE_BLE_ERROR,
                    BLE_FAILED_ADVERTISING
                )
            )
        }
    }
    private val bluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            Log.d(
                TAG,
                "GattServer callback - onCharacteristicWriteRequest $requestId of characteristic ${characteristic?.uuid} "
            )
            if (value == null) {
                return
            }

            if (device != null) {
                bleAdapter.sendResponse(device, requestId)
            }

            read(value)
        }

        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            Log.d(
                TAG,
                "GattServer callback - onConnectionStateChange with state $newState for device $device and status $status"
            )
            if (device == null) {
                return
            }

            if (newState == BluetoothGattServer.STATE_CONNECTED) {
                bluetoothDevice = device
                if (state != State.Advertising) return
                state = State.Connecting
                val connected = bleAdapter.connect(device)
                if (connected) {
                    bleAdapter.stopAdvertise(advertisingCallback)
                }
            } else if (newState == BluetoothGattServer.STATE_DISCONNECTED) {
                listener?.onCloseUnexpectedly(
                    Error(
                        GeotabDriveError.MODULE_BLE_ERROR,
                        BLE_DISCONNECTED
                    )
                )
                stopBle(advertisingCallback)
            }
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor?
        ) {
            Log.d(
                TAG,
                "GattServer callback - onDescriptorReadRequest$requestId,$offset "
            )
            if (device != null) {
                bleAdapter.sendResponse(device, requestId)
            }
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            Log.d(
                TAG, "#onNotificationSent $status ,  $device"
            )
            super.onNotificationSent(device, status)
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            Log.d(
                TAG,
                "GattServer callback - onDescriptorWriteRequest- requestid, offset - $requestId,$offset "
            )

            if (device == null || value == null || device != bluetoothDevice) {
                return
            }

            if (state != State.Connecting) return

            if (value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                bleAdapter.sendResponse(device, requestId)
                listener?.onOpen(null)
                state = State.Connected
            }
        }

        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            Log.d(TAG, "Ble gatt server callback - onMtuChanged $device of characteristic $mtu")
            super.onMtuChanged(device, mtu)
        }
    }

    override fun read(byteArray: ByteArray) {
        if (state == State.Connected && !byteArray.contentEquals(HANDSHAKE.data) && !byteArray.contentEquals(
                ACK.data
            )
        ) {
            parseDataReceivedOnConnected(byteArray)
        } else {
            listener?.onRead(byteArray)
        }
    }

    private fun parseDataReceivedOnConnected(data: ByteArray) {
        when (val deviceData = partialGoDeviceData) {
            null -> {
                if (isGoDeviceData(data)) {
                    val dataLength = (data[2].toInt()) + 6
                    // create new device data with info received
                    partialGoDeviceData = PartialGoDeviceData(data, dataLength)
                }
            }
            else -> {
                // append to existing device data available
                partialGoDeviceData =
                    PartialGoDeviceData(deviceData.data + data, deviceData.length)
            }
        }

        val deviceData = partialGoDeviceData ?: return
        if (deviceData.data.size == deviceData.length) {
            partialGoDeviceData = null
            listener?.onRead(deviceData.data)
        }
    }

    private fun stopBle(advertisingCallback: AdvertiseCallback) {
        Log.d(IoxBleModule.TAG, "Stopping Ble")
        bleAdapter.stopAdvertise(advertisingCallback)
        bluetoothDevice?.let { bleAdapter.cancelConnection(it) }
        bleAdapter.stopServer()
        bleAdapter.unregisterBleStateOffCallback()
        state = State.Idle
    }

    data class PartialGoDeviceData(val data: ByteArray, val length: Int) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PartialGoDeviceData

            if (!data.contentEquals(other.data)) return false
            if (length != other.length) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + length
            return result
        }
    }
}
