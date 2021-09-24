package com.geotab.mobile.sdk.module.iox.ioxUsb

import android.hardware.usb.UsbAccessory
import com.geotab.mobile.sdk.Error
import java.io.InputStream
import java.io.OutputStream

interface UsbAdapter {
    val accessoryList: Array<UsbAccessory>
    fun dispatchOnMain(runnable: () -> Unit)
    fun dispatchIO(runnable: () -> Unit)
    fun hasPermission(accessory: UsbAccessory): Boolean
    fun open(accessory: UsbAccessory, connectionCallback: (exception: Error) -> Unit): Pair<InputStream, OutputStream>?
    fun close(accessory: UsbAccessory)
    fun requestPermission(accessory: UsbAccessory, permissionCallback: (Boolean) -> Unit)
    fun cancelPermissionRequest()
}
