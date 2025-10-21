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
data class ReadFileAsBinaryArgument(
    val path: String?,
    val offset: Long?,
    val size: Long?
)

class ReadFileAsBinaryFunction(val context: Context, override val name: String = "readFileAsBinary", override val module: FileSystemModule) :
    ModuleFunction, BaseFunction<ReadFileAsBinaryArgument>() {

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
            if (fileOptions.path == null) {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }
            // check offset and size are not negative values, otherwise throw error
            if ((fileOptions.offset?.let { it < 0 } == true) || (fileOptions.size?.let { it < 0 } == true)) {
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

            val path = fileUtils.validateUrlAndReturnPath(fileOptions.path) ?: run {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }

            // File exist
            if (!fileUtils.isExist(path, rootUri)) {
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

            var fileBytes: ByteArray? = null
            try {
                fileBytes = fileUtils.readFile(path, rootUri, fileOptions.offset ?: 0, fileOptions.size)
                // Directly convert byte array to a JSON array string for performance.
                val jsonArrayString = fileBytes.joinToString(separator = ",", prefix = "[", postfix = "]")
                jsCallback(Success("new Uint8Array($jsonArrayString).buffer"))
            } catch (e: Error) {
                jsCallback(Failure(e))
            } finally {
                // Securely clear the byte array from memory after use.
                fileBytes?.fill(0)
            }
        }
    }

    override fun getType(): Type {
        return object : TypeToken<ReadFileAsBinaryArgument>() {}.type
    }
}
