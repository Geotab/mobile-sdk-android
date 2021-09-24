package com.geotab.mobile.sdk.permission

interface PermissionDelegate {
    fun askPermissionsResult(permissions: Array<Permission>, callback: (Boolean) -> Unit)
}
