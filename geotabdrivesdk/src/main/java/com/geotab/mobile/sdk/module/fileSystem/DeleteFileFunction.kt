package com.geotab.mobile.sdk.module.fileSystem

import android.content.Context
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.util.FileUtils
import kotlinx.coroutines.launch

class DeleteFileFunction(val context: Context, override val name: String = "deleteFile", override val module: FileSystemModule) :
    ModuleFunction {
    private val fileUtils by lazy {
        FileUtils(context)
    }
    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        module.launch {
            val pathStr = jsonString?.takeIf { it.isNotBlank() } ?: run {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }
            val rootUri = module.drvfsRootUri ?: run {
                jsCallback(Failure(Error(GeotabDriveError.FILE_EXCEPTION, FileSystemModule.FILESYSTEM_NOT_EXIST)))
                return@launch
            }

            val pathUrl = fileUtils.validateUrlAndReturnPath(pathStr) ?: run {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR, FileSystemModule.INVALID_FILE_PATH + pathStr)))
                return@launch
            }

            // File is a directory
            if (fileUtils.isDirectory(pathUrl, rootUri)) {
                jsCallback(
                    Failure(
                        Error(
                            GeotabDriveError.FILE_EXCEPTION,
                            FileSystemModule.FILE_IS_DIRECTORY
                        )
                    )
                )
                return@launch
            }

            // File exist
            val fileToDelete = fileUtils.getFile(pathUrl, rootUri) ?: run {
                jsCallback(
                    Failure(
                        Error(
                            GeotabDriveError.FILE_EXCEPTION,
                            FileSystemModule.FILE_NOT_EXIST
                        )
                    )
                )
                return@launch
            }

            // read from file
            try {
                (fileToDelete).delete().takeIf { it }?.let {
                    jsCallback(Success("undefined"))
                    return@launch
                } ?: run {
                    throw Error(GeotabDriveError.FILE_EXCEPTION)
                }
            } catch (e: Exception) {
                val message = e.message?.let { ": $it" } ?: ""
                jsCallback(
                    Failure(
                        Error(
                            GeotabDriveError.FILE_EXCEPTION,
                            FileSystemModule.DELETEFILE_FAIL + message
                        )
                    )
                )
                return@launch
            }
        }
    }
}
