package com.geotab.mobile.sdk.module.iox.ioxUsb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.ParcelFileDescriptor
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class UsbAdapterDefault(private val context: Context) : UsbAdapter {

    private companion object {
        const val ACTION_USB_PERMISSION = "android.geotab.ioxusbmanager.action.USB_PERMISSION"
    }

    private class Permission(var accessory: UsbAccessory, var pendingIntent: PendingIntent, var permissionCallback: (Boolean) -> Unit)

    private class Connection(var accessory: UsbAccessory, var fileDescriptor: ParcelFileDescriptor, var connectionCallback: ((exception: Error) -> Unit))

    private val usbManager: UsbManager
        get() = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var connection: Connection? = null

    private var permission: Permission? = null

    private var receiver: Receiver? = null

    override val accessoryList: Array<UsbAccessory>
        get() = usbManager.accessoryList ?: emptyArray()

    override fun dispatchIO(runnable: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch { runnable() }
    }

    override fun dispatchOnMain(runnable: () -> Unit) {
        MainScope().launch { runnable() }
    }

    override fun hasPermission(accessory: UsbAccessory): Boolean = usbManager.hasPermission(accessory)

    override fun open(
        accessory: UsbAccessory,
        connectionCallback: (exception: Error) -> Unit
    ): Pair<InputStream, OutputStream>? {
        return when (val descriptor = usbManager.openAccessory(accessory)) {
            null -> null
            else -> {
                when (val fileDescriptor = descriptor.fileDescriptor) {
                    null -> null
                    else -> {
                        registerReceiver()
                        connection = Connection(accessory, descriptor, connectionCallback)
                        Pair(FileInputStream(fileDescriptor), FileOutputStream(descriptor.fileDescriptor))
                    }
                }
            }
        }
    }

    override fun close(accessory: UsbAccessory) {
        connection?.let {
            if (it.accessory == accessory) {
                connection = null
                it.fileDescriptor.close()
                unregisterReceiver()
            }
        }
    }

    override fun requestPermission(accessory: UsbAccessory, permissionCallback: (Boolean) -> Unit) {
        registerReceiver()
        val intent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), 0)
        permission = Permission(accessory, intent, permissionCallback)
        usbManager.requestPermission(accessory, intent)
    }

    override fun cancelPermissionRequest() {
        permission?.let {
            permission = null
            unregisterReceiver()
            it.pendingIntent.cancel()
        }
    }

    private fun registerReceiver() {
        if (receiver == null) {
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
            receiver = Receiver()
            context.registerReceiver(receiver, filter)
        }
    }

    private fun unregisterReceiver() {
        receiver?.let {
            if (connection == null && permission == null) {
                context.unregisterReceiver(it)
                receiver = null
            }
        }
    }

    inner class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.also {
                val accessory = intent.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)
                if (intent.action == ACTION_USB_PERMISSION) {
                    permission?.let {
                        val isPermitted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        if (isPermitted && it.accessory == accessory) {
                            permission = null
                        }
                        it.permissionCallback(isPermitted)
                    }
                } else if (intent.action == UsbManager.ACTION_USB_ACCESSORY_DETACHED) {
                    connection?.let {
                        connection = null
                        unregisterReceiver()
                        it.fileDescriptor.close()
                        it.connectionCallback(Error(GeotabDriveError.SOCKET_ACCESSORY_DETACHED_EXCEPTION))
                    }
                }
            }
        }
    }
}
