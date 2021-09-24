package com.geotab.mobile.sdk.module.browser

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentManager
import com.geotab.mobile.sdk.module.Module
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class BrowserModule(
    fragmentManager: FragmentManager,
    context: Context,
    override val name: String = "browser"
) : Module(name), CoroutineScope {
    companion object {
        const val templateFileName = "BrowserModule.Script.js"
        const val openFunctionName = "openBrowserWindow"
        const val closeFunctionName = "closeBrowserWindow"
        const val ERROR_GETTING_MAPS_APP_IN_DEVICE = "No Google Play Services or maps apps installed in the device"
        const val BROWSER_TAG = "BROWSER"
    }
    private val fsExecutor = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val fsContext: CoroutineScope = CoroutineScope(fsExecutor)

    override val coroutineContext: CoroutineContext
        get() = fsContext.coroutineContext
    var intent: Intent? = null
    init {
        functions.add(
            OpenBrowserWindowFunction(
                module = this,
                context = context,
                fragmentManager = fragmentManager
            )
        )
        functions.add(CloseBrowserWindowFunction(module = this, context = context))
    }

    override fun scripts(context: Context): String {

        var scripts = super.scripts(context)
        // create hashmap to populate values into script
        val scriptParameter: HashMap<String, Any> =
            hashMapOf(
                "moduleName" to name,
                "closeFunctionName" to closeFunctionName,
                "openFunctionName" to openFunctionName,
                "geotabModules" to geotabModules
            )
        scripts += getScriptFromTemplate(context, templateFileName, scriptParameter)
        return scripts
    }
}
