package com.geotab.mobile.sdk.module.iox.ioxBle

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
import com.geotab.mobile.sdk.module.iox.DeviceEventTransformer
import com.geotab.mobile.sdk.module.iox.GeotabIoxClient
import com.geotab.mobile.sdk.module.iox.MainExecuter
import com.geotab.mobile.sdk.permission.PermissionDelegate
import com.geotab.mobile.sdk.permission.PermissionHelper
import com.geotab.mobile.sdk.util.JsonUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors.newSingleThreadExecutor
import kotlin.coroutines.CoroutineContext

class IoxBleModule(
    private var ioxClient: GeotabIoxClient,
    private val push: (ModuleEvent, ((Result<Success<String>, Failure>) -> Unit)) -> Unit,
    override val name: String = "ioxble"
) : Module(name), GeotabIoxClient.Listener, CoroutineScope {
    var deviceEventCallback: (Result<Success<String>, Failure>) -> Unit = {}
    constructor(
        context: Context,
        permissionDelegate: PermissionDelegate,
        push: (ModuleEvent, ((Result<Success<String>, Failure>) -> Unit)) -> Unit
    ) : this(
        GeotabIoxClient(
            SocketAdapterBleDefault(context, PermissionHelper(context, permissionDelegate)),
            DeviceEventTransformer(),
            MainExecuter()
        ),
        push
    )

    companion object {
        const val TAG = "BLE_MODULE"
        const val BLE_ALREADY_PROCESSING = "BLE connection is already in progress"
    }

    private val fsExecutor = newSingleThreadExecutor().asCoroutineDispatcher()
    private val fsContext: CoroutineScope = CoroutineScope(fsExecutor)

    override val coroutineContext: CoroutineContext
        get() = fsContext.coroutineContext

    var startCallback: ((Result<Success<String>, Failure>) -> Unit)? = null

    init {
        functions.add(
            StartIoxBleFunction(
                module = this
            )
        )
        functions.add(
            StopIoxBleFunction(
                module = this
            )
        )
    }

    fun start(uuidStr: String, jsCallback: (Result<Success<String>, Failure>) -> Unit) {
        if (startCallback != null) {
            jsCallback(
                Failure(
                    Error(
                        GeotabDriveError.MODULE_BLE_ERROR,
                        BLE_ALREADY_PROCESSING
                    )
                )
            )
            return
        }

        ioxClient.uuidStr = uuidStr
        startCallback = jsCallback
        ioxClient.start(this)
    }

    private fun callStartCallback(result: Result<Success<String>, Failure>) {
        startCallback?.let {
            it(result)
        } ?: return
        startCallback = null
    }

    fun stop(jsCallback: (Result<Success<String>, Failure>) -> Unit) {
        startCallback = null
        ioxClient.stop()
        ioxClient.uuidStr = null
        jsCallback(Success("undefined"))
    }

    override fun onStart(exception: Error?) {
        if (exception != null) {
            Log.e(TAG, "IoxBle Client failed to start ${exception.getErrorCode()}", exception)
            onStoppedUnexpectedly(exception)
        } else {
            callStartCallback(Success("undefined"))
        }
    }

    override fun onStoppedUnexpectedly(exception: Error) {
        if (startCallback != null) {
            callStartCallback(
                Failure(
                    Error(
                        GeotabDriveError.MODULE_BLE_ERROR,
                        exception.getErrorMessage()
                    )
                )
            )
        } else {
            val errorDetail = ErrorDetail(exception.getErrorMessage())
            push(ModuleEvent("ioxble.error", JsonUtil.toJson(errorDetail))) {}
        }
    }

    override fun onEvent(deviceEvent: DeviceEvent?, exception: Error?) {
        exception?.let {
            Log.e(TAG, "IoxBle Client event exception ${it.getErrorCode()}", it)
            val errorDetail = ErrorDetail(it.getErrorMessage())
            // pushing it to Drive
            push(ModuleEvent("ioxble.error", JsonUtil.toJson(errorDetail))) {}
            // pushing it to external implementors callback
            deviceEventCallback(Failure(Error(GeotabDriveError.JS_ISSUED_ERROR, JsonUtil.toJson(errorDetail))))
        }
        deviceEvent?.let {
            // pushing it to Drive
            push(
                ModuleEvent(
                    "ioxble.godevicedata",
                    JsonUtil.toJson(DeviceEventDetail(deviceEvent))
                )
            ) {}

            // pushing it to external implementors callback
            deviceEventCallback(Success(JsonUtil.toJson(IOXDeviceEvent(IOXType.BLE.id, deviceEvent))))
        }
    }

    override fun onDisconnect() {
        Log.d(TAG, "Iox device disconnected")
    }
}
