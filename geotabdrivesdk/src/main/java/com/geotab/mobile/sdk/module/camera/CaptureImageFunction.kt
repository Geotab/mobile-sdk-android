package com.geotab.mobile.sdk.module.camera

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
data class CaptureImageFunctionArgument(
    val size: Size? = null,
    val fileName: String? = null
)
class CaptureImageFunction(
    override val name: String = "captureImage",
    val context: Context,
    val cameraDelegate: CameraDelegate,
    val moduleContainerDelegate: ModuleContainerDelegate,
    override val module: CameraModule
) :
    ModuleFunction {

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        val arguments = jsonString?.let {
            try {
                JsonUtil.fromJson<CaptureImageFunctionArgument>(it)
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
        val cameraLauncher = CameraLauncher(cameraDelegate, moduleContainerDelegate, arguments, context, jsCallback)
        cameraLauncher.dispatchTakePictureIntent()
    }
}
