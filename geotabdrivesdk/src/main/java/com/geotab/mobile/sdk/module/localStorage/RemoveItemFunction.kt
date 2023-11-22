package com.geotab.mobile.sdk.module.localStorage

import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.database.localStorage.LocalStorageRepository
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.localStorage.LocalStorageModule.Companion.ERROR_REMOVING_KEY
import com.geotab.mobile.sdk.util.JsonUtil
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.lang.reflect.Type

class RemoveItemFunction(
    override val name: String = "removeItem",
    val localStorageRepository: LocalStorageRepository,
    override val module: LocalStorageModule
) : ModuleFunction, BaseFunction<String>() {

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        module.launch(module.coroutineContext) {
            val arguments = transformOrInvalidate(jsonString, jsCallback)
                ?: return@launch
            val result = localStorageRepository.delete(arguments)
            if (result == 0) {
                jsCallback(Failure(Error(GeotabDriveError.STORAGE_MODULE_ERROR, ERROR_REMOVING_KEY)))
                return@launch
            }
            jsCallback(Success(JsonUtil.toJson(arguments)))
        }
    }

    override fun getType(): Type {
        return object : TypeToken<String>() {}.type
    }
}
