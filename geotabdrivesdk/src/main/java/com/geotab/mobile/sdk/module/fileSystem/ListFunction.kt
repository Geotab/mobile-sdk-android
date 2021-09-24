package com.geotab.mobile.sdk.module.fileSystem

import android.content.Context
import androidx.annotation.Keep
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.util.FileUtils
import com.geotab.mobile.sdk.util.JsonUtil
import kotlinx.coroutines.launch

@Keep
data class FileInfo(
    val size: Long?,
    val name: String,
    val isDir: Boolean,
    val modifiedDate: String
)

class ListFunction(val context: Context, override val name: String = "list", override val module: FileSystemModule) :
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

            val path = fileUtils.validatePath(pathStr, false) ?: run {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }

            // File exist
            val dirFile = fileUtils.getFile(path, rootUri) ?: run {
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

            // File is not a directory
            if (!fileUtils.isDirectory(path, rootUri)) {
                jsCallback(
                    Failure(
                        Error(
                            GeotabDriveError.FILE_EXCEPTION,
                            FileSystemModule.FILE_IS_NOT_DIRECTORY
                        )
                    )
                )
                return@launch
            }

            try {
                val files: List<FileInfo> = fileUtils.getFiles(dirFile)
                val result = JsonUtil.toJson(files)
                jsCallback(Success(result))
            } catch (e: Exception) {
                val message = e.message?.let { ": $it" } ?: ""
                jsCallback(Failure(Error(GeotabDriveError.FILE_EXCEPTION, FileSystemModule.LIST_FAIL + message)))
                return@launch
            }
        }
    }
}
