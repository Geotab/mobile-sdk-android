package com.geotab.mobile.sdk.util

import android.net.Uri
import androidx.core.net.toUri

class UriWrapper {
    fun parse(pathString: String): Uri {
        return pathString.toUri()
    }
}
