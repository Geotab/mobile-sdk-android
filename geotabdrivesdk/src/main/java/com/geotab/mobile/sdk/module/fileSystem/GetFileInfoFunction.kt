package com.geotab.mobile.sdk.module.fileSystem

import android.content.Context
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.util.FileUtils
import com.geotab.mobile.sdk.util.JsonUtil
import kotlinx.coroutines.launch

class GetFileInfoFunction(val context: Context, override val name: String = "getFileInfo", override val module: FileSystemModule) :
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

            val uriPath = fileUtils.getUriPath(pathStr) ?: run {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR, FileSystemModule.INVALID_FILE_PATH + pathStr)))
                return@launch
            }

            val path = fileUtils.validatePath(pathStr, !fileUtils.isDirectory(uriPath, rootUri)) ?: run {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }

            // File exist
            val file = fileUtils.getFile(path, rootUri) ?: run {
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

            try {
                val fileInfo = fileUtils.getFileInfo(file)
                val result = JsonUtil.toJson(fileInfo)
                jsCallback(Success(result))
            } catch (e: Exception) {
                val message = e.message?.let { ": $it" } ?: ""
                jsCallback(Failure(Error(GeotabDriveError.FILE_EXCEPTION, FileSystemModule.GET_FILE_INFO_FAIL + message)))
                return@launch
            }
        }
    }
}
