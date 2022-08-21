package com.geotab.mobile.sdk.fileChooser

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient

interface FileChooserDelegate {
    fun uploadFileResult(filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: WebChromeClient.FileChooserParams?)
}
