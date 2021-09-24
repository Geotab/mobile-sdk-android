package com.geotab.mobile.sdk.models

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.geotab.mobile.sdk.BuildConfig
import java.util.UUID

private const val PLATFORM_VAL = "Android"
private const val UUID_KEY = "UUID"
data class Device(
    val context: Context,
    val preferences: SharedPreferences,
    val platform: String = PLATFORM_VAL
) {
    val model: String = Build.MODEL
    val uuid: String
        get() {
            var uuid = preferences.getString(UUID_KEY, null)
            if (uuid == null) {
                val editor = preferences.edit()
                uuid = UUID.randomUUID().toString()
                editor.putString(UUID_KEY, uuid)
                editor.apply()
            }
            return uuid
        }

    var appId: String = context.packageName
    val manufacturer: String = Build.MANUFACTURER
    val sdkVersion = BuildConfig.VERSION_NAME
}
