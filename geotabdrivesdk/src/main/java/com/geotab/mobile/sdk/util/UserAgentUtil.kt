package com.geotab.mobile.sdk.util

import android.content.Context
import com.geotab.mobile.sdk.BuildConfig

class UserAgentUtil(context: Context) {
    private val info = context.packageManager.getApplicationInfo(context.packageName, 0)
    val appName: String = context.packageManager.getApplicationLabel(info).toString()
    val version: String = context.packageManager.getPackageInfo(context.packageName, 0).versionName

    fun getUserAgent(prefixUserAgent: String = ""): String {
        return if (prefixUserAgent.isBlank()) {
            "MobileSDK/${(BuildConfig.VERSION_NAME)} $appName/$version"
        } else {
            "$prefixUserAgent MobileSDK/${(BuildConfig.VERSION_NAME)} $appName/$version"
        }
    }
}
