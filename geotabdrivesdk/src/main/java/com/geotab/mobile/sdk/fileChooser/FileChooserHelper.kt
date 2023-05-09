package com.geotab.mobile.sdk.fileChooser

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

private const val ANY_FILE = "*/*"
private const val TAG = "FileChooserHelper"

class FileChooserHelper(private val fragment: Fragment) {
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var fileChooserParams: WebChromeClient.FileChooserParams? = null

    fun chooseFile(filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: WebChromeClient.FileChooserParams?) {
        this.filePathCallback = filePathCallback
        this.fileChooserParams = fileChooserParams
        try {
            startFileChoose.launch(
                ""
            )
        } catch (exception: ActivityNotFoundException) {
            Log.e(TAG, exception.message, exception)
        }
    }

    private val startFileChoose =
        fragment.registerForActivityResult(object : ActivityResultContracts.GetMultipleContents() {
            override fun createIntent(context: Context, input: String): Intent {
                val intent = super.createIntent(context, input)
                if (fileChooserParams?.acceptTypes?.size?.let { it > 1 } == true) {
                    intent.type = ANY_FILE
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, fileChooserParams?.acceptTypes)
                } else {
                    intent.type = when (val fileType = fileChooserParams?.acceptTypes?.first() ?: ANY_FILE) {
                        "" -> ANY_FILE
                        else -> fileType
                    }
                }
                return intent
            }
        }) {
        filePathCallback?.onReceiveValue(it.toTypedArray())
    }
}
