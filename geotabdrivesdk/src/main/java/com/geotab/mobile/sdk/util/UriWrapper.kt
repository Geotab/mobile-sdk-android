package com.geotab.mobile.sdk.util

import android.net.Uri

class UriWrapper {
    fun parse(pathString: String): Uri {
        return Uri.parse(pathString)
    }
}
