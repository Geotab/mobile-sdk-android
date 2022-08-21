package com.geotab.mobile.sdk.fileChooser

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient

class FileChooserHelper(private val fileChooserDelegate: FileChooserDelegate) {

    fun chooseFile(filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: WebChromeClient.FileChooserParams?) {
        fileChooserDelegate.uploadFileResult(filePathCallback, fileChooserParams)
    }
}
