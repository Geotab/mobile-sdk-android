package com.geotab.mobile.sdk.module.secureStorage

import com.geotab.mobile.sdk.models.database.secureStorage.SecureStorageRepository
import com.geotab.mobile.sdk.module.Module
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class SecureStorageModule(
    repository: SecureStorageRepository
) : Module(MODULE_NAME), CoroutineScope {
    companion object {
        const val MODULE_NAME = "secureStorage"
        const val ERROR_GETTING_VALUE = "Key not exists in storage"
        const val ERROR_REMOVING_ALL = "Nothing to clear"
        const val ERROR_REMOVING_KEY = "Error in removing from storage"
        const val ERROR_KEY_EMPTY = "Key cannot be null or empty"
        const val ERROR_VALUE_EMPTY = "Value cannot be null or empty"
    }

    private val fsExecutor = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val fsContext: CoroutineScope = CoroutineScope(fsExecutor)

    init {
        functions.add(SetItemFunction(module = this, secureStorageRepository = repository))
        functions.add(GetItemFunction(module = this, secureStorageRepository = repository))
        functions.add(RemoveItemFunction(module = this, secureStorageRepository = repository))
        functions.add(ClearFunction(module = this, secureStorageRepository = repository))
        functions.add(KeysFunction(module = this, secureStorageRepository = repository))
        functions.add(LengthFunction(module = this, secureStorageRepository = repository))
    }
    override val coroutineContext: CoroutineContext
        get() = fsContext.coroutineContext
}
