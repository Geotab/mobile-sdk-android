package com.geotab.mobile.sdk.permission

enum class Permission(val request: String) {
    LOCATION("android.permission.ACCESS_FINE_LOCATION"),
    LOCATION_COARSE("android.permission.ACCESS_COARSE_LOCATION"),
    WRITE_EXTERNAL("android.permission.WRITE_EXTERNAL_STORAGE"),
    ACTIVITY_RECOGNITION("android.permission.ACTIVITY_RECOGNITION"),
    CAMERA("android.permission.CAMERA"),
    BLUETOOTH_CONNECT("android.permission.BLUETOOTH_CONNECT"),
    BLUETOOTH_ADVERTISE("android.permission.BLUETOOTH_ADVERTISE"),
    POST_NOTIFICATIONS("android.permission.POST_NOTIFICATIONS"),
}
