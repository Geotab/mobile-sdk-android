package com.geotab.mobile.sdk.module.secureStorage

import androidx.annotation.Keep
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.database.secureStorage.SecureStorageRepository
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.util.JsonUtil
import kotlinx.coroutines.launch
import org.json.JSONObject

@Keep
data class SetItemArguments(val key: String, val value: String)

class SetItemFunction(
    override val name: String = "setItem",
    val secureStorageRepository: SecureStorageRepository,
    override val module: SecureStorageModule
) : ModuleFunction {

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        module.launch(module.coroutineContext) {
            var valueChars: CharArray? = null
            if (jsonString.isNullOrBlank()) {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }
            try {
                val jsonObject = JSONObject(jsonString)
                val key = jsonObject.optString("key", null)
                if (key.isNullOrBlank()) {
                    jsCallback(Failure(Error(GeotabDriveError.JS_ISSUED_ERROR, SecureStorageModule.ERROR_KEY_EMPTY)))
                    return@launch
                } else if (!jsonObject.has("value") || jsonObject.getString("value").isBlank()) {
                    jsCallback(Failure(Error(GeotabDriveError.JS_ISSUED_ERROR, SecureStorageModule.ERROR_VALUE_EMPTY)))
                    return@launch
                }
                valueChars = jsonObject.getString("value").toCharArray()

                secureStorageRepository.insertOrUpdate(key, valueChars)
                jsCallback(Success(JsonUtil.toJson(key)))
            } catch (e: Exception) {
                jsCallback(Failure(Error(GeotabDriveError.STORAGE_MODULE_ERROR, e.message)))
            } finally {
                valueChars?.fill('\u0000')
            }
        }
    }
}
