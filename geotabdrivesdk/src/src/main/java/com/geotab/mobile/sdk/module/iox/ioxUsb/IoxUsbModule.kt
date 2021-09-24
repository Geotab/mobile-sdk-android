package com.geotab.mobile.sdk.module.iox.ioxUsb

import android.content.Context
import android.util.Log
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.DeviceEvent
import com.geotab.mobile.sdk.models.DeviceEventDetail
import com.geotab.mobile.sdk.models.ModuleEvent
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.iox.AsyncMainExecuterAdapter
import com.geotab.mobile.sdk.module.iox.DeviceEventTransformer
import com.geotab.mobile.sdk.module.iox.GeotabIoxClient
import com.geotab.mobile.sdk.module.iox.MainExecuter
import com.geotab.mobile.sdk.module.iox.SocketAdapter
import com.geotab.mobile.sdk.util.JsonUtil

class IoxUsbModule(
    var context: Context,
    private val push: (ModuleEvent) -> Unit,
    override val name: String = "ioxusb"
) : Module(name), GeotabIoxClient.Listener {
    private val transformer = DeviceEventTransformer()
    private val socketUsbAdapterDefault: SocketAdapter = SocketAdapterUsbDefault(context)
    private val executerAdapter: AsyncMainExecuterAdapter = MainExecuter()
    private val ioxClient: GeotabIoxClient = GeotabIoxClient(socketUsbAdapterDefault, transformer, executerAdapter)

    private companion object {
        const val TAG = "IoxUsbModule"
    }

    fun start() {
        ioxClient.start(this)
    }

    fun stop() {
        ioxClient.stop()
    }

    override fun onStart(exception: Error?) {
        exception?.let { Log.e(TAG, "Iox Client failed to start ${it.getErrorCode()}", it) }
    }

    override fun onStoppedUnexpectedly(exception: Error) {
        Log.e(TAG, "Iox Client stopped", exception)
        when (val errorCode = exception.getErrorCode()) {
            GeotabDriveError.SOCKET_WRITE_EXCEPTION, GeotabDriveError.SOCKET_ACCESSORY_DETACHED_EXCEPTION -> {
                Log.d(TAG, "Restarting Iox Client")
                ioxClient.start(this)
            }
            else -> Log.d(TAG, errorCode.toString())
        }
    }

    override fun onEvent(deviceEvent: DeviceEvent?, exception: Error?) {
        exception?.let { Log.e(TAG, "Iox Client event exception ${it.getErrorCode()}", it) }
        deviceEvent?.let {
            push(ModuleEvent("iox.data", JsonUtil.toJson(DeviceEventDetail(deviceEvent))))
        }
    }
}
