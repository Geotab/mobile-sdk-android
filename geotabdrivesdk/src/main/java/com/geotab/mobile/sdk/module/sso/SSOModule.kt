package com.geotab.mobile.sdk.module.sso

import androidx.fragment.app.FragmentManager
import com.geotab.mobile.sdk.module.Module
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class SSOModule(
    fragmentManager: FragmentManager
) : Module(MODULE_NAME), CoroutineScope {

    companion object {
        const val SAML_LAUNCHING_BROWSER = "Closing to launch another browser"
        const val MODULE_NAME = "sso"
    }

    private val fsExecutor = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val fsContext: CoroutineScope = CoroutineScope(fsExecutor)
    override val coroutineContext: CoroutineContext
        get() = fsContext.coroutineContext

    init {
        functions.add(
            SamlLoginFunction(
                fragmentManager = fragmentManager,
                module = this
            )
        )
    }
}
