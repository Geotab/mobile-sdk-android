package com.geotab.mobile.sdk.module.camera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.Keep

@Keep
data class GetPictureAttribute(
    val imageUri: Uri,
    var result: Boolean = false,
    val callback: (Boolean) -> Unit
)
class TakePictureContract : ActivityResultContract<GetPictureAttribute, GetPictureAttribute>() {
    private lateinit var pictureAttribute: GetPictureAttribute

    override fun createIntent(context: Context, input: GetPictureAttribute): Intent {
        this.pictureAttribute = input
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, input.imageUri)
    }

    override fun getSynchronousResult(
        context: Context,
        input: GetPictureAttribute
    ): SynchronousResult<GetPictureAttribute>? {
        return null
    }

    override fun parseResult(resultCode: Int, intent: Intent?): GetPictureAttribute {
        if (resultCode == Activity.RESULT_OK) {
            pictureAttribute.result = true
        }
        return pictureAttribute
    }
}
