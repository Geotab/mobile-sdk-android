package com.geotab.mobile.sdk.module.localStorage

import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.database.localStorage.LocalStorage
import com.geotab.mobile.sdk.models.database.localStorage.LocalStorageRepository
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

data class SetItemArguments(val key: String, val value: String)

class SetItemFunction(
    override val name: String = "setItem",
    val localStorageRepository: LocalStorageRepository,
    override val module: LocalStorageModule
) : ModuleFunction, BaseFunction<SetItemArguments>() {

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        module.launch(module.coroutineContext) {
            val arguments = transformOrInvalidate(jsonString, jsCallback) ?: return@launch

            if (arguments.key.isBlank()) {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }
            try {
                val encryptedStorage = LocalStorage(
                    arguments.key,
                    encryptText(module.keyAlias, arguments.value)
                )
                localStorageRepository.insertOrUpdate(encryptedStorage)
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
