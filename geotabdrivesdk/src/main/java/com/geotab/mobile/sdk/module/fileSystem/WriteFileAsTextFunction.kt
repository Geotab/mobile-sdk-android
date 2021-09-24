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
import com.geotab.mobile.sdk.module.fileSystem.FileSystemModule.Companion.CREATEDIRECTORY_FAIL
import com.geotab.mobile.sdk.module.fileSystem.FileSystemModule.Companion.FILESYSTEM_NOT_EXIST
import com.geotab.mobile.sdk.util.FileUtils
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.lang.reflect.Type

@Keep
data class WriteFileAsTextArgument(
    val path: String?,
    val data: String?,
    val offset: Long?
)

class WriteFileAsTextFunction(val context: Context, override val name: String = "writeFileAsText", override val module: FileSystemModule) :
    ModuleFunction, BaseFunction<WriteFileAsTextArgument>() {
    private val fileUtils by lazy {
        FileUtils(context)
    }

    override fun handleJavascriptCall(jsonString: String?, jsCallback: (Result<Success<String>, Failure>) -> Unit) {
        module.launch {
            val fileOptions = transformOrInvalidate(jsonString, jsCallback)
                ?: return@launch
            // Check if the Gson Transformer converts to null value.
            if (fileOptions.path == null || fileOptions.data == null) {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }

            val rootUri = module.drvfsRootUri ?: run {
                jsCallback(Failure(Error(GeotabDriveError.FILE_EXCEPTION, FILESYSTEM_NOT_EXIST)))
                return@launch
            }

            val path = fileUtils.validateUrlAndReturnPath(fileOptions.path) ?: run {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }

            // create directory, if required. On failure to create, report exception
            if (!fileUtils.createDirectory(path, rootUri)) {
                jsCallback(
                    Failure(
                        Error(
                            GeotabDriveError.FILE_EXCEPTION,
                            "$CREATEDIRECTORY_FAIL $path"
                        )
                    )
                )
                return@launch
            }

            // write to file
            try {
                fileOptions.data.toByteArray(Charsets.UTF_8)
                    .apply {
                        fileUtils.writeToFile(this, path, rootUri, fileOptions.offset).let {
                            jsCallback(Success("$it"))
                            return@launch
                        }
                    }
            } catch (e: Error) {
                jsCallback(Failure(e))
                return@launch
            }
        }
    }

    override fun getType(): Type {
        return object : TypeToken<WriteFileAsTextArgument>() {}.type
    }
}
