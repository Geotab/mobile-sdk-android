package com.geotab.mobile.sdk.util

import android.R.attr.version
import android.content.Context
import com.geotab.mobile.sdk.BuildConfig

class UserAgentUtil(context: Context) {
    private val info = context.packageManager.getApplicationInfo(context.packageName, 0)
    private val version: String = context.packageManager.getPackageInfo(context.packageName, 0).versionName
    val appName: String = context.packageManager.getApplicationLabel(info).toString()
    val sdkVersion: String = BuildConfig.VERSION_NAME

    fun getUserAgent(prefixUserAgent: String = ""): String {
        return if (prefixUserAgent.isBlank()) {
            "MobileSDK/$sdkVersion $appName/$version"
        } else {
            "$prefixUserAgent MobileSDK/$sdkVersion $appName/$version"
        }
    }
}
