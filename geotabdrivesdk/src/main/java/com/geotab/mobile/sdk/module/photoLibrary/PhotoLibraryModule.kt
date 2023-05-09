package com.geotab.mobile.sdk.module.photoLibrary

import android.content.Context
import com.geotab.mobile.sdk.ModuleContainerDelegate
import com.geotab.mobile.sdk.module.Module

class PhotoLibraryModule(
    val context: Context,
    photoLibraryDelegate: PhotoLibraryDelegate,
    moduleContainerDelegate: ModuleContainerDelegate
) : Module(MODULE_NAME) {
    companion object {
        const val PROCESS_IMAGE_ERROR = "Error in processing image "
        const val FILE_ALREADY_EXIST = "Given filename already exists"
        const val MODULE_NAME = "photoLibrary"
    }
    init {
        functions.add(PickImageFunction(context = context, photoLibraryDelegate = photoLibraryDelegate, moduleContainerDelegate = moduleContainerDelegate, module = this))
    }
}
