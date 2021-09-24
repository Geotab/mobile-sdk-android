package com.geotab.mobile.sdk.module

import android.content.Context
import androidx.annotation.Keep
import com.geotab.mobile.sdk.Error
import java.io.Serializable

/**
 * Base type for handling JavaScript function calls
 */
@Keep
interface ModuleFunction : Serializable {
    val name: String
    val module: Module

    /**
     * Handle incoming JavaScript calls from Geotab Drive's web component
     *
     * @param jsonString object from JavaScript caller to parse
     * @param jsCallback callback to notify JavaScript caller of [Success] or [Failure]
     */
    @Keep
    fun handleJavascriptCall(jsonString: String?, jsCallback: (Result<Success<String>, Failure>) -> Unit)
    @Keep
    fun getScriptData(): HashMap<String, Any> {
        return hashMapOf(
            "geotabModules" to Module.geotabModules,
            "moduleName" to module.name,
            "functionName" to name,
            "geotabNativeCallbacks" to Module.geotabNativeCallbacks,
            "callbackPrefix" to Module.callbackPrefix,
            "interfaceName" to Module.interfaceName
        )
    }
    @Keep
    fun scripts(context: Context): String {
        return module.getScriptFromTemplate(context, "ModuleFunction.Script.js", getScriptData())
    }
}

sealed class Result<out T, out U>
data class Success<T>(val value: T) : Result<Success<T>, Nothing>()
data class Failure(val reason: Error) : Result<Nothing, Failure>()
