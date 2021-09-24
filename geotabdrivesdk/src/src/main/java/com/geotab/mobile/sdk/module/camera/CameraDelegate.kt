package com.geotab.mobile.sdk.module.camera

import android.net.Uri

interface CameraDelegate {
    fun takePictureResult(imageUri: Uri, callback: (Boolean) -> Unit)
}
