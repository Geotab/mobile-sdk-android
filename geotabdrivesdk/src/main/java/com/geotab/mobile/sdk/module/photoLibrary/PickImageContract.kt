package com.geotab.mobile.sdk.module.photoLibrary

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper
import androidx.annotation.Keep

@Keep
data class PickImageAttribute(
    val input: String,
    var uri: Uri?,
    val callback: (Uri?) -> Unit
)
class PickImageContract : ActivityResultContract<PickImageAttribute, PickImageAttribute>() {
    private lateinit var pictureAttribute: PickImageAttribute

    @CallSuper
    override fun createIntent(context: Context, pickImageAttribute: PickImageAttribute): Intent {
        this.pictureAttribute = pickImageAttribute
        return Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(pickImageAttribute.input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PickImageAttribute {
        if (intent != null && resultCode == Activity.RESULT_OK) {
            pictureAttribute.uri = intent.data
        }
        return pictureAttribute
    }
}
