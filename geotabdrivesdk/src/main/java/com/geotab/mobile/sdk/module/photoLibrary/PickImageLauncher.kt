package com.geotab.mobile.sdk.module.photoLibrary

import android.content.Context
import android.net.Uri
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.ModuleContainerDelegate
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.fileSystem.FileSystemModule
import com.geotab.mobile.sdk.util.ImageUtil

class PickImageLauncher(
    private val photoLibraryDelegate: PhotoLibraryDelegate,
    private val moduleContainerDelegate: ModuleContainerDelegate,
    val context: Context,
    val callback: (Result<Success<String>, Failure>) -> Unit
) {
    private val imageUtil: ImageUtil by lazy {
        ImageUtil(context)
    }

    fun pickImage(
        arguments: PickImageFunctionArgument?
    ) {
        try {
            val fsModule = moduleContainerDelegate.findModule("fileSystem") as? FileSystemModule
            val fsRootUri = fsModule?.drvfsRootUri ?: run {
                callback(
                    Failure(
                        Error(
                            GeotabDriveError.FILE_EXCEPTION,
                            FileSystemModule.FILESYSTEM_NOT_EXIST
                        )
                    )
                ); return
            }
            val imageFile = imageUtil.createImageFile(fsRootUri, arguments?.fileName)
            if (imageFile.exists()) {
                callback(
                    Failure(
                        Error(
                            GeotabDriveError.PICK_IMAGE_ERROR,
                            PhotoLibraryModule.FILE_ALREADY_EXIST
                        )
                    )
                ); return
            }
            val destUri = Uri.fromFile(imageFile)

            photoLibraryDelegate.pickImageResult { uri ->
                uri?.let {
                    processImageFromGallery(it, destUri, arguments).let { uri ->
                        imageUtil.toDrvfsUri(fsRootUri, uri)?.let { path ->
                            // return image url in the callback
                            callback(Success("\"${path}\""))
                        } ?: run {
                            throw java.lang.Exception("Error in fetching drvfs uri")
                        }
                    }
                } ?: run {
                    callback(Failure(Error(GeotabDriveError.PICK_IMAGE_ERROR, "No Image picked")))
                }
            }
        } catch (e: Exception) {
            callback(
                Failure(
                    Error(
                        GeotabDriveError.PICK_IMAGE_ERROR,
                        "${PhotoLibraryModule.PROCESS_IMAGE_ERROR}:${e.message}"
                    )
                )
            )
            return
        }
    }

    private fun processImageFromGallery(
        uri: Uri,
        destUri: Uri,
        pickImageFunctionArgument: PickImageFunctionArgument?
    ): Uri {
        var finalUri = destUri
        val rotation = imageUtil.getImageOrientation(uri)
        imageUtil.rotateAndScaleImage(
            uri,
            pickImageFunctionArgument?.size,
            rotation,
            destUri
        )?.let { resultUri ->
            finalUri = resultUri
        } ?: run {
            throw Exception("Error in processing image from gallery")
        }
        return finalUri
    }
}
