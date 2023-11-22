package com.geotab.mobile.sdk.module.localStorage

import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.database.localStorage.LocalStorageRepository
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.util.JsonUtil
import com.geotab.mobile.sdk.util.decryptText
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.lang.reflect.Type

class GetItemFunction(
    override val name: String = "getItem",
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

            try {
                val byteResult = localStorageRepository.getValue(arguments)
                if (byteResult == null) {
                    jsCallback(
                        Failure(
                            Error(
                                GeotabDriveError.STORAGE_MODULE_ERROR,
                                LocalStorageModule.ERROR_GETTING_VALUE
                            )
                        )
                    )
                    return@launch
                }
                val result = decryptText(module.keyAlias, byteResult)
                jsCallback(Success(JsonUtil.toJson(result)))
            } catch (e: Exception) {
                jsCallback(Failure(Error(GeotabDriveError.STORAGE_MODULE_ERROR, e.message)))
                return@launch
            }
        }
    }

    override fun getType(): Type {
        return object : TypeToken<String>() {}.type
    }
}
