package com.geotab.mobile.sdk.module.secureStorage

import androidx.annotation.Keep
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.database.secureStorage.SecureStorage
import com.geotab.mobile.sdk.models.database.secureStorage.SecureStorageRepository
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.util.JsonUtil
import com.geotab.mobile.sdk.util.encryptText
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.lang.reflect.Type

@Keep
data class SetItemArguments(val key: String, val value: String)

class SetItemFunction(
    override val name: String = "setItem",
    val secureStorageRepository: SecureStorageRepository,
    override val module: SecureStorageModule
) : ModuleFunction, BaseFunction<SetItemArguments>() {

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        module.launch(module.coroutineContext) {
            val arguments = transformOrInvalidate(jsonString, jsCallback) ?: return@launch

            if (arguments.key.isNullOrBlank()) {
                jsCallback(Failure(Error(GeotabDriveError.JS_ISSUED_ERROR, SecureStorageModule.ERROR_KEY_EMPTY)))
                return@launch
            } else if (arguments.value.isNullOrBlank()) {
                jsCallback(Failure(Error(GeotabDriveError.JS_ISSUED_ERROR, SecureStorageModule.ERROR_VALUE_EMPTY)))
                return@launch
            }

            try {
                val encryptedStorage = SecureStorage(
                    arguments.key,
                    encryptText(module.keyAlias, arguments.value)
                )
                secureStorageRepository.insertOrUpdate(encryptedStorage)
                jsCallback(Success(JsonUtil.toJson(arguments.key)))
            } catch (e: Exception) {
                jsCallback(Failure(Error(GeotabDriveError.STORAGE_MODULE_ERROR, e.message)))
            }
        }
    }

    override fun getType(): Type {
        return object : TypeToken<SetItemArguments>() {}.type
    }
}
