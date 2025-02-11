package com.geotab.mobile.sdk.module.camera
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.ModuleContainerDelegate
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.fileSystem.FileSystemModule
import com.geotab.mobile.sdk.util.ImageUtil
import java.io.File

class CameraLauncher(
    private val cameraDelegate: CameraDelegate,
    private val moduleContainerDelegate: ModuleContainerDelegate,
    private val captureImageFunctionArgument: CaptureImageFunctionArgument?,
    val context: Context,
    val callback: (Result<Success<String>, Failure>) -> Unit
) {
    companion object {
        const val TAG = "CameraLauncher"
    }
    private val applicationId = context.applicationInfo.packageName
    private lateinit var imageUri: Uri
    private lateinit var fsRootUri: Uri
    private var exceptionThrown = false
    private val imageUtil: ImageUtil by lazy {
        ImageUtil(context)
    }

    fun dispatchTakePictureIntent() {

        exceptionThrown = false
        val fsModule = moduleContainerDelegate.findModule(FileSystemModule.MODULE_NAME) as? FileSystemModule
        fsRootUri = fsModule?.drvfsRootUri ?: run {
            callback(Failure(Error(GeotabDriveError.FILE_EXCEPTION, FileSystemModule.FILESYSTEM_NOT_EXIST))); return
        }

        val hasCameras =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) &&
                getNumberOfCameras() > 0

        if (hasCameras) {
            try {
                val imageFile = imageUtil.createImageFile(fsRootUri, captureImageFunctionArgument?.fileName)
                if (imageFile.exists()) {
                    callback(Failure(Error(GeotabDriveError.CAPTURE_IMAGE_ERROR, CameraModule.FILE_ALREADY_EXIST))); return
                }
                val destUri = Uri.fromFile(imageFile)
                val photo: File = imageUtil.createImageFile(fsRootUri)
                // Specify file so that image is captured and returned
                imageUri = FileProvider.getUriForFile(
                    context,
                    "$applicationId.provider",
                    photo
                )
                cameraDelegate.takePictureResult(imageUri) { result ->
                    if (result) {
                        processImageFromCamera(fsRootUri, destUri)
                    } else {
                        if (!exceptionThrown) {
                            callback(
                                Failure(
                                    Error(
                                        GeotabDriveError.CAPTURE_IMAGE_ERROR,
                                        CameraModule.CAMERA_CANCELLED
                                    )
                                )
                            )
                        }
                    }
                }
            } catch (ex: Exception) {
                exceptionThrown = true
                callback(
                    Failure(
                        Error(
                            GeotabDriveError.CAPTURE_IMAGE_ERROR,
                            "${CameraModule.DISPATCH_INTENT_ERROR}:${ex.message}"
                        )
                    )
                )
                return
            }
        } else {
            Log.e(TAG, "Device doesn't have a Camera to capture the picture")
            callback(
                Failure(
                    Error(
                        GeotabDriveError.CAPTURE_IMAGE_ERROR,
                        CameraModule.CAMERA_NOT_AVAILABLE
                    )
                )
            )
        }
    }

    private fun processImageFromCamera(fsRootUri: Uri, destUri: Uri) {
        try {
            var finalUri = destUri
            val rotation = imageUtil.getImageOrientation(imageUri)

            imageUtil.rotateAndScaleImage(
                imageUri,
                captureImageFunctionArgument?.size,
                rotation,
                finalUri
            )?.let { uri ->
                finalUri = uri
            } ?: run {
                throw Exception("Error in processing image")
            }

            imageUtil.toDrvfsUri(fsRootUri, finalUri)?.let {
                // return image url in the callback
                callback(Success("\"${it}\""))
            } ?: run {
                throw Exception("Error in fetching drvfs uri")
            }
        } catch (e: Exception) {
            callback(
                Failure(
                    Error(
                        GeotabDriveError.CAPTURE_IMAGE_ERROR,
                        "${CameraModule.PROCESS_IMAGE_ERROR}:${e.message}"
                    )
                )
            )
        } finally {
            // delete the original
            context.contentResolver.delete(imageUri, null, null)
        }
    }

    private fun getNumberOfCameras(): Int {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return try {
            cameraManager.cameraIdList.size
        } catch (e: Exception) {
            Log.e(TAG, "Error in getting number of cameras: ${e.stackTraceToString()}")
            0 // return 0 if there is an exception, meaning no cameras
        }
    }
}
