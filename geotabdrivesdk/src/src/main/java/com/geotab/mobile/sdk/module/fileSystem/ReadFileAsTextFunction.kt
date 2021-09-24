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
import com.geotab.mobile.sdk.util.JsonUtil
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.lang.reflect.Type

@Keep
data class ReadFileAsTextArgument(
    val path: String?
)
class ReadFileAsTextFunction(val context: Context, override val name: String = "readFileAsText", override val module: FileSystemModule) :
    ModuleFunction, BaseFunction<ReadFileAsTextArgument>() {
    private val fileUtils by lazy {
        FileUtils(context)
    }

    override fun handleJavascriptCall(jsonString: String?, jsCallback: (Result<Success<String>, Failure>) -> Unit) {
        module.launch {
            val fileOptions = transformOrInvalidate(jsonString, jsCallback)
                ?: return@launch
            // Check if the Gson Transformer converts to null value.
            if (fileOptions.path == null) {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }
            val rootUri = module.drvfsRootUri ?: run {
                jsCallback(Failure(Error(GeotabDriveError.FILE_EXCEPTION, FileSystemModule.FILESYSTEM_NOT_EXIST)))
                return@launch
            }

            val path = fileUtils.validateUrlAndReturnPath(fileOptions.path) ?: run {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }

            // File exist
            val fileToRead = fileUtils.getFile(path, rootUri) ?: run {
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
                (fileToRead).readText(Charsets.UTF_8).let {
                    val textString = JsonUtil.toJson(it)
                    jsCallback(Success(textString))
                    return@launch
                }
            } catch (e: Exception) {
                val message = e.message?.let { ": $it" } ?: ""
                jsCallback(Failure(Error(GeotabDriveError.FILE_EXCEPTION, FileSystemModule.READFILE_FAIL + message)))
                return@launch
            }
        }
    }

    override fun getType(): Type {
        return object : TypeToken<ReadFileAsTextArgument>() {}.type
    }
}
