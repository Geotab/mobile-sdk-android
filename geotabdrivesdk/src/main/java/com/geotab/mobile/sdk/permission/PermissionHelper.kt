package com.geotab.mobile.sdk.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class PermissionHelper(val context: Context, private val permissionDelegate: PermissionDelegate) {
    companion object {
        const val PERMISSION_RESPONSE = "PERMISSION_RESPONSE"
        const val PERMISSION_GRANTED = "PERMISSION_GRANTED"
        const val PERMISSION_DENIED = "PERMISSION_DENIED"
    }

    fun hasPermission(permissions: Array<Permission>): Boolean =
        permissions.all {
            ContextCompat.checkSelfPermission(
                context, it.request
            ) == PackageManager.PERMISSION_GRANTED
        }

    fun requestPermission(permissions: Array<Permission>, callback: (Boolean) -> Unit) {
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

    fun getLocationPermissionsBasedOnAndroidApi() = if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
        arrayOf(Permission.LOCATION)
    } else {
        arrayOf(Permission.LOCATION, Permission.LOCATION_COARSE)
    }
}
