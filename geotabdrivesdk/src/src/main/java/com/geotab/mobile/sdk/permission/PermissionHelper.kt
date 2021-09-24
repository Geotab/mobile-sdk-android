package com.geotab.mobile.sdk.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class PermissionHelper(val context: Context, private val permissionDelegate: PermissionDelegate) {
    companion object {
        const val PERMISSION_RESPONSE = "PERMISSION_RESPONSE"
        const val PERMISSION_GRANTED = "PERMISSION_GRANTED"
        const val PERMISSION_DENIED = "PERMISSION_DENIED"
    }

    fun hasPermission(permissions: Array<Permission>): Boolean =
        permissions.all {
            ActivityCompat.checkSelfPermission(
                context, it.request
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun requestPermission(permissions: Array<Permission>, callback: (Boolean) -> Unit) {
        permissionDelegate.askPermissionsResult(permissions, callback)
    }

    fun checkPermission(permissions: Array<Permission>, callback: (Boolean) -> Unit) {
        if (hasPermission(permissions)) {
            return callback(true)
        } else {
            requestPermission(permissions) { isSuccess ->
                callback(isSuccess)
            }
        }
    }
}
