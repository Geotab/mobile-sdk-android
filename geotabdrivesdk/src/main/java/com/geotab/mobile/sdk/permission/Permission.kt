package com.geotab.mobile.sdk.permission

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
enum class Permission(val request: String) : Parcelable {
    LOCATION("android.permission.ACCESS_FINE_LOCATION"),
    LOCATION_COARSE("android.permission.ACCESS_COARSE_LOCATION"),
    WRITE_EXTERNAL("android.permission.WRITE_EXTERNAL_STORAGE"),
    CAMERA("android.permission.CAMERA"),
    BLUETOOTH_CONNECT("android.permission.BLUETOOTH_CONNECT"),
    BLUETOOTH_ADVERTISE("android.permission.BLUETOOTH_ADVERTISE"),
    POST_NOTIFICATIONS("android.permission.POST_NOTIFICATIONS"),
}
