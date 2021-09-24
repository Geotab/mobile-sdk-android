package com.geotab.mobile.sdk.models

import android.os.Build

data class AppDeviceAddReq(
    val appUUID: String,
    val appId: String,
    val platform: String = "Android",
    val model: String = Build.MODEL,
    val notificationToken: String,
    val expireAfterDays: Int = 14
)
