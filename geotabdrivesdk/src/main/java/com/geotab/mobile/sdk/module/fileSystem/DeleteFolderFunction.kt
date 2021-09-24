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
data class DeleteFolderArgument(
    val path: String?
)

class DeleteFolderFunction(
    val context: Context,
    override val name: String = "deleteFolder",
    override val module: FileSystemModule
) : ModuleFunction, BaseFunction<DeleteFolderArgument>() {

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

            val rootFolderForComparison = "${FileSystemModule.fsPrefix}:///"

            if (pathStr == rootFolderForComparison) {
                jsCallback(
                    Failure(
                        Error(
                            GeotabDriveError.FILE_EXCEPTION,
                            "${FileSystemModule.CANNOT_DELETE_ROOT_FOLDER}$pathStr"
                        )
                    )
                )
                return@launch
            }

            val folder =
                fileUtils.validatePath(uriString = pathStr, isFile = false, rootUri = rootUri)
                    ?: run {
                        jsCallback(
                            Failure(
                                Error(
                                    GeotabDriveError.FILE_EXCEPTION,
                                    "${FileSystemModule.INVALID_PATH}$pathStr"
                                )
                            )
                        )
                        return@launch
                    }

            if (!folder.isDirectory) {
                jsCallback(
                    Failure(
                        Error(
                            GeotabDriveError.FILE_EXCEPTION,
                            "${FileSystemModule.PATH_IS_NOT_FOLDER}$pathStr"
                        )
                    )
                )
                return@launch
            }

            if (!fileUtils.isFolderEmpty(folder)) {
                jsCallback(
                    Failure(
                        Error(
                            GeotabDriveError.FILE_EXCEPTION,
                            "${FileSystemModule.FOLDER_NOT_EMPTY}$pathStr"
                        )
                    )
                )
                return@launch
            }

            try {
                if (folder.delete()) {
                    jsCallback(Success("undefined"))
                    return@launch
                } else {
                    throw Error(
                        GeotabDriveError.FILE_EXCEPTION,
                        "${FileSystemModule.DELETE_FOLDER_FAIL}$pathStr"
                    )
                }
            } catch (e: Error) {
                jsCallback(Failure(e))
                return@launch
            }
        }
    }

    override fun getType(): Type {
        return object : TypeToken<DeleteFolderArgument>() {}.type
    }
}
