package com.geotab.mobile.sdk

import androidx.annotation.Keep

@Keep
class DriveSdkConfig {
    @Keep
    companion object {
        const val apiCallTimeoutMilli = 9 * 1000L
        var serverAddress = "my.geotab.com"
        var allowThirdPartyCookies = false
        var includeAppAuthModules = false
    }
}
