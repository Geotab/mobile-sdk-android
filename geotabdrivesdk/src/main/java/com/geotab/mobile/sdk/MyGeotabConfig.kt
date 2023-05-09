package com.geotab.mobile.sdk

import androidx.annotation.Keep

@Keep
class MyGeotabConfig {
    @Keep
    companion object {
        var serverAddress = "my.geotab.com"
        var allowThirdPartyCookies = false
    }
}
