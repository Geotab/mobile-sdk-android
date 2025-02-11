package com.geotab.mobile.sdk.module.iox.ioxUsb

import android.content.Context
import android.util.Log
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.DeviceEvent
import com.geotab.mobile.sdk.models.DeviceEventDetail
import com.geotab.mobile.sdk.models.ErrorDetail
import com.geotab.mobile.sdk.models.IOXDeviceEvent
import com.geotab.mobile.sdk.models.ModuleEvent
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.models.enums.IOXType
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.iox.AsyncMainExecuterAdapter
import com.geotab.mobile.sdk.module.iox.DeviceEventTransformer
import com.geotab.mobile.sdk.module.iox.GeotabIoxClient
import com.geotab.mobile.sdk.module.iox.MainExecuter
import com.geotab.mobile.sdk.module.iox.SocketAdapter
import com.geotab.mobile.sdk.util.JsonUtil

class IoxUsbModule(
    var context: Context,
    private val push: (ModuleEvent, ((Result<Success<String>, Failure>) -> Unit)) -> Unit
) : Module(MODULE_NAME), GeotabIoxClient.Listener {
    var deviceEventCallback: (Result<Success<String>, Failure>) -> Unit = {}
    private val transformer = DeviceEventTransformer()
    private val socketUsbAdapterDefault: SocketAdapter = SocketAdapterUsbDefault(context)
    private val executerAdapter: AsyncMainExecuterAdapter = MainExecuter()
    private val ioxClient: GeotabIoxClient = GeotabIoxClient(socketUsbAdapterDefault, transformer, executerAdapter)

    private companion object {
        const val TAG = "IoxUsbModule"
        const val IOX_CONNECTION = "iox.data"
        const val IOX_ATTACHED = "{ detail: { attached: true } }"
        const val IOX_DETACHED = "{ detail: { attached: false } }"
        const val MODULE_NAME = "ioxusb"
    }

    fun start() {
        ioxClient.start(this)
    }

    fun stop() {
        ioxClient.stop()
    }

    override fun onStart(state: GeotabIoxClient.State, exception: Error?) {
        if (exception == null && state == GeotabIoxClient.State.Connected) {
            push(ModuleEvent(IOX_CONNECTION, IOX_ATTACHED)) {}
        } else if (exception != null) {
            Log.e(TAG, "Iox Client failed to start ${exception.getErrorCode()}", exception)
        }
    }

    override fun onStoppedUnexpectedly(exception: Error) {
        Log.e(TAG, "Iox Client stopped", exception)
        onDisconnect()
        when (val errorCode = exception.getErrorCode()) {
            GeotabDriveError.SOCKET_WRITE_EXCEPTION, GeotabDriveError.SOCKET_ACCESSORY_DETACHED_EXCEPTION -> {
                Log.d(TAG, "Restarting Iox Client")
                ioxClient.start(this)
            }
            else -> Log.d(TAG, errorCode.toString())
        }
    }

    override fun onEvent(deviceEvent: DeviceEvent?, exception: Error?) {
        exception?.let {
            val errorDetail = ErrorDetail(it.getErrorMessage())
            // pushing it to external implementors callback
            deviceEventCallback(Failure(Error(GeotabDriveError.JS_ISSUED_ERROR, JsonUtil.toJson(errorDetail))))
            Log.e(TAG, "Iox Client event exception ${it.getErrorCode()}", it)
        }
        deviceEvent?.let {
            push(ModuleEvent(IOX_CONNECTION, JsonUtil.toJson(DeviceEventDetail(deviceEvent)))) {}
            // pushing it to external implementors callback
            deviceEventCallback(Success(JsonUtil.toJson(IOXDeviceEvent(IOXType.USB.id, deviceEvent))))
        }
    }

    override fun onDisconnect() {
        Log.d(TAG, "Iox device disconnected")
        push(ModuleEvent(IOX_CONNECTION, IOX_DETACHED)) {}
    }

    override fun onStateUpdate(state: GeotabIoxClient.State) {
        Log.d(TAG, "Iox state updated")
    }
}
