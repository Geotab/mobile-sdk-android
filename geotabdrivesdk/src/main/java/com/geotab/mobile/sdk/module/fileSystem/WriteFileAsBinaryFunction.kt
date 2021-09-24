package com.geotab.mobile.sdk.module.fileSystem

import android.content.Context
import androidx.annotation.Keep
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Module
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.util.FileUtils
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.lang.reflect.Type

@Keep
data class WriteFileAsBinaryArgument(
    val path: String?,
    val data: ByteArray?,
    val offset: Long?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WriteFileAsBinaryArgument

        if (path != other.path) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        if (offset != other.offset) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path?.hashCode() ?: 0
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + (offset?.hashCode() ?: 0)
        return result
    }
}

class WriteFileAsBinaryFunction(val context: Context, override val name: String = "writeFileAsBinary", override val module: FileSystemModule) :
    ModuleFunction, BaseFunction<WriteFileAsBinaryArgument>() {
    companion object {
        const val templateFileName = "ModuleFunction.WriteFileAsBinary.Script.js"
    }
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

            if (fileOptions.path == null || fileOptions.data == null) {
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

            // create directory, if required. On failure to create, report exception
            if (!fileUtils.createDirectory(path, rootUri)) {
                jsCallback(
                    Failure(
                        Error(
                            GeotabDriveError.FILE_EXCEPTION,
                            "${FileSystemModule.CREATEDIRECTORY_FAIL} $path"
                        )
                    )
                )
                return@launch
            }

            // write to file
            try {
                fileUtils.writeToFile(fileOptions.data, path, rootUri, fileOptions.offset).let {
                    jsCallback(Success("$it"))
                    return@launch
                }
            } catch (e: Error) {
                jsCallback(Failure(e))
                return@launch
            }
        }
    }

    override fun getType(): Type {
        return object : TypeToken<WriteFileAsBinaryArgument>() {}.type
    }

    override fun scripts(context: Context): String {
        // create hashmap to populate values into script
        val scriptParameter: HashMap<String, Any> =
            hashMapOf(
                "moduleName" to module.name,
                "functionName" to name,
                "interfaceName" to Module.interfaceName,
                "geotabModules" to Module.geotabModules,
                "callbackPrefix" to Module.callbackPrefix,
                "geotabNativeCallbacks" to Module.geotabNativeCallbacks
            )

        return module.getScriptFromTemplate(context, templateFileName, scriptParameter)
    }
}
