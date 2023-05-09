package com.geotab.mobile.sdk.module.camera

import android.content.Context
import com.geotab.mobile.sdk.ModuleContainerDelegate
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.permission.PermissionDelegate
import com.geotab.mobile.sdk.permission.PermissionHelper

class CameraModule(
    context: Context,
    cameraDelegate: CameraDelegate,
    permissionDelegate: PermissionDelegate,
    moduleContainerDelegate: ModuleContainerDelegate
) : Module(MODULE_NAME) {
    val permissionHelper: PermissionHelper by lazy {
        PermissionHelper(context, permissionDelegate)
    }

    companion object {
        const val CAMERA_NOT_AVAILABLE = "Device doesn\'t have a camera "
        const val FILE_ALREADY_EXIST = "Given filename already exists"
        const val CAMERA_CANCELLED = "Capture Image action cancelled"
        const val PROCESS_IMAGE_ERROR = "Error in processing image "
        const val DISPATCH_INTENT_ERROR = "Error in dispatching camera intent"
        const val FILENAME_FORMAT = "yyyyMMdd_HHmmssSSS"
        const val ENCODING_TYPE_EXT = ".png"
        const val PERMISSION_DENIED = "PERMISSION_DENIED"
        const val MODULE_NAME = "camera"
    }
    init {
        functions.add(CaptureImageFunction(context = context, cameraDelegate = cameraDelegate, moduleContainerDelegate = moduleContainerDelegate, module = this))
    }
}
