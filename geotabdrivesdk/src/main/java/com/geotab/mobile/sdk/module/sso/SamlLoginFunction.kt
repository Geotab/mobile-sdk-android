package com.geotab.mobile.sdk.module.sso

import androidx.annotation.Keep
import androidx.fragment.app.FragmentManager
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.browser.BrowserFragment
import com.geotab.mobile.sdk.module.browser.BrowserModule
import com.geotab.mobile.sdk.util.replaceFragment
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.reflect.Type

@Keep
data class SamlLoginArgument(val samlLoginUrl: String)

class SamlLoginFunction(
    private val fragmentManager: FragmentManager,
    override val module: SSOModule,
    override val name: String = "samlLogin"
) : ModuleFunction, BaseFunction<SamlLoginArgument>() {

    private var samlCallback: ((Result<Success<String>, Failure>) -> Unit)? = null

    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {

        module.launch(Dispatchers.Main) {
            val arguments = transformOrInvalidate(jsonString, jsCallback) ?: return@launch

            // Check if the Gson Transformer converts to null value.
            @Suppress("SENSELESS_COMPARISON")
            if (arguments.samlLoginUrl.isNullOrBlank()) {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }

            samlCallback = jsCallback

            val moduleScripts = "window.sessionStorage.getItem('geotab_sso_credentials')"
            val browserFragment = BrowserFragment.newInstance(arguments.samlLoginUrl)

            browserFragment.script = moduleScripts
            browserFragment.samlCallback = samlCallback
            replaceFragment(browserFragment, BrowserModule.BROWSER_TAG, fragmentManager)
        }
    }

    override fun getType(): Type {
        return object : TypeToken<SamlLoginArgument>() {}.type
    }
}
