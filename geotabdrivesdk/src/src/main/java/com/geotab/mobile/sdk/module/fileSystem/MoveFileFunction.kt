package com.geotab.mobile.sdk.module.fileSystem

import android.content.Context
import androidx.annotation.Keep
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.util.FileUtils
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.lang.reflect.Type

@Keep
data class MoveFileArgument(
    val srcPath: String?,
    val dstPath: String?,
    val overwrite: Boolean?
)

class MoveFileFunction(
    val context: Context,
    override val name: String = "moveFile",
    override val module: FileSystemModule
) : ModuleFunction, BaseFunction<MoveFileArgument>() {

    private val fileUtils by lazy {
        FileUtils(context)
    }

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        module.launch {
            val fileOptions = transformOrInvalidate(jsonString, jsCallback)
                ?: return@launch

            // Check if the Gson Transformer converts to null value.
            if (fileOptions.srcPath == null) {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }

            // Check if the Gson Transformer converts to null value.
            if (fileOptions.dstPath == null) {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }

            val rootUri = module.drvfsRootUri ?: run {
                jsCallback(
                    Failure(
                        Error(
                            GeotabDriveError.FILE_EXCEPTION,
                            FileSystemModule.FILESYSTEM_NOT_EXIST
                        )
                    )
                )
                return@launch
            }

            val srcPath = fileUtils.validateUrlAndReturnPath(fileOptions.srcPath) ?: run {
                jsCallback(
                    Failure(
                        Error(
                            GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR,
                            FileSystemModule.INVALID_FILE_PATH + fileOptions.srcPath
                        )
                    )
                )
                return@launch
            }

            val dstPath = fileUtils.validateUrlAndReturnPath(fileOptions.dstPath) ?: run {
                jsCallback(
                    Failure(
                        Error(
                            GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR,
                            FileSystemModule.INVALID_FILE_PATH + fileOptions.dstPath
                        )
                    )
                )
                return@launch
            }

            val overwrite = fileOptions.overwrite ?: false

            // File is a directory
            if (fileUtils.isDirectory(srcPath, rootUri)) {
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

            // File is a directory
            if (fileUtils.isDirectory(dstPath, rootUri)) {
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
            val fileToMove = fileUtils.getFile(srcPath, rootUri) ?: run {
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

            // create directory, if required. On failure to create, report exception
            if (!fileUtils.createDirectory(dstPath, rootUri)) {
                jsCallback(
                    Failure(
                        Error(
                            GeotabDriveError.FILE_EXCEPTION,
                            "${FileSystemModule.CREATEDIRECTORY_FAIL} $dstPath"
                        )
                    )
                )
                return@launch
            }

            // File exist
            val destinationFile = fileUtils.getOrCreateFile(dstPath, rootUri)

            try {
                when (overwrite) {
                    true -> {
                        if (destinationFile != null &&
                            fileToMove.renameTo(destinationFile)
                        ) {
                            jsCallback(Success("undefined"))
                            return@launch
                        } else {
                            throw Error(GeotabDriveError.FILE_EXCEPTION)
                        }
                    }

                    false -> {
                        if (destinationFile != null &&
                            !destinationFile.exists() &&
                            fileToMove.renameTo(destinationFile)
                        ) {
                            jsCallback(Success("undefined"))
                            return@launch
                        } else {
                            throw Error(GeotabDriveError.FILE_EXCEPTION, "${FileSystemModule.FILE_ALREADY_EXISTS}$dstPath")
                        }
                    }
                }
            } catch (e: Exception) {
                val message = e.message?.let { ": $it" } ?: ""
                jsCallback(
                    Failure(
                        Error(
                            GeotabDriveError.FILE_EXCEPTION,
                            FileSystemModule.MOVE_FILE_FAIL + message
                        )
                    )
                )
                return@launch
            }
        }
    }

    override fun getType(): Type {
        return object : TypeToken<MoveFileArgument>() {}.type
    }
}
