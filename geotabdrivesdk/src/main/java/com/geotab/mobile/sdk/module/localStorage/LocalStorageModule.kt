package com.geotab.mobile.sdk.module.localStorage

import android.content.Context
import com.geotab.mobile.sdk.BuildConfig
import com.geotab.mobile.sdk.models.database.AppDatabase
import com.geotab.mobile.sdk.models.database.localStorage.LocalStorageRepository
import com.geotab.mobile.sdk.module.Module
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class LocalStorageModule(val context: Context) : Module(MODULE_NAME), CoroutineScope {
    companion object {
        const val MODULE_NAME = "localstorage"
        const val ERROR_GETTING_VALUE = "Key not exists in storage"
        const val ERROR_REMOVING_ALL = "Nothing to clear"
        const val ERROR_REMOVING_KEY = "Error in removing from storage"
    }

    private val fsExecutor = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val fsContext: CoroutineScope = CoroutineScope(fsExecutor)
    private val repository by lazy {
        LocalStorageRepository(AppDatabase.getDatabase(context).localStorageDao())
    }
    val keyAlias = context.applicationInfo.packageName + "." + BuildConfig.KEYSTORE_ALIAS

    init {
        functions.add(SetItemFunction(module = this, localStorageRepository = repository))
        functions.add(GetItemFunction(module = this, localStorageRepository = repository))
        functions.add(RemoveItemFunction(module = this, localStorageRepository = repository))
        functions.add(ClearFunction(module = this, localStorageRepository = repository))
        functions.add(KeysFunction(module = this, localStorageRepository = repository))
        functions.add(LengthFunction(module = this, localStorageRepository = repository))
    }
    override val coroutineContext: CoroutineContext
        get() = fsContext.coroutineContext
}
