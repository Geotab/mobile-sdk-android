package com.geotab.mobile.sdk.permission

enum class Permission(val request: String) {
    LOCATION("android.permission.ACCESS_FINE_LOCATION"),
    WRITE_EXTERNAL("android.permission.WRITE_EXTERNAL_STORAGE"),
    ACTIVITY_RECOGNITION("android.permission.ACTIVITY_RECOGNITION")
}
