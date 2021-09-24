package com.geotab.mobile.sdk.module.photoLibrary

import android.content.Context
import androidx.annotation.Keep
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.ModuleContainerDelegate
import com.geotab.mobile.sdk.models.Size
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.util.ImageUtil.Companion.MAX_HEIGHT
import com.geotab.mobile.sdk.util.ImageUtil.Companion.MAX_WIDTH
import com.geotab.mobile.sdk.util.JsonUtil

@Keep
data class PickImageFunctionArgument(
    val size: Size? = null,
    val fileName: String? = null
)
class PickImageFunction(
    override val name: String = "pickImage",
    val context: Context,
    val photoLibraryDelegate: PhotoLibraryDelegate,
    val moduleContainerDelegate: ModuleContainerDelegate,
    override val module: PhotoLibraryModule
) :
    ModuleFunction {
    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        val arguments = jsonString?.let {
            try {
                JsonUtil.fromJson<PickImageFunctionArgument>(it)
            } catch (e: Exception) {
                null
            }
        }
        arguments?.size?.let {
            if (it.width <= 0 || it.height <= 0 || it.width > MAX_WIDTH || it.height > MAX_HEIGHT) {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return
            }
        }
        val pickImageLauncher = PickImageLauncher(
            photoLibraryDelegate = photoLibraryDelegate,
            moduleContainerDelegate = moduleContainerDelegate,
            context = context,
            callback = jsCallback
        )
        pickImageLauncher.pickImage(arguments)
    }
}
