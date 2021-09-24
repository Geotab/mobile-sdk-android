package com.geotab.mobile.sdk.module.photoLibrary

import android.net.Uri

interface PhotoLibraryDelegate {
    fun pickImageResult(callback: (Uri?) -> Unit)
}
