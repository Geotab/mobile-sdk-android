package com.geotab.mobile.sdk.module.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.Keep
import androidx.fragment.app.FragmentManager
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.BaseFunction
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.ModuleFunction
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.browser.BrowserModule.Companion.openFunctionName
import com.geotab.mobile.sdk.module.browser.HtmlTarget.Companion.getTarget
import com.geotab.mobile.sdk.util.replaceFragment
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.reflect.Type

enum class HtmlTarget(val target: String) {
    Blank("_blank"),
    Self("_self"),
    Parent("_parent"),
    Top("_top"),
    Iab("iab");

    companion object {
        fun getTarget(targetStr: String?): HtmlTarget? =
            values().firstOrNull { it.target == targetStr }
    }
}

@Keep
data class OpenBrowserWindowArgument(val url: String, val target: String?, val features: String?)

class OpenBrowserWindowFunction(
    override val name: String = openFunctionName,
    override val module: BrowserModule,
    private val context: Context,
    private val fragmentManager: FragmentManager
) : ModuleFunction, BaseFunction<OpenBrowserWindowArgument>() {
    override fun handleJavascriptCall(
        jsonString: String?,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        module.launch(Dispatchers.Main) {
            val arguments = transformOrInvalidate(jsonString, jsCallback) ?: return@launch

            // Check if the Gson Transformer converts to null value.
            @Suppress("SENSELESS_COMPARISON")
            if (arguments.url == null) {
                jsCallback(Failure(Error(GeotabDriveError.MODULE_FUNCTION_ARGUMENT_ERROR)))
                return@launch
            }

            val urlString = arguments.url
            val targetString = arguments.target

            when (getTarget(targetString)) {
                HtmlTarget.Blank,
                null -> openBrowser(
                    url = urlString,
                    external = true,
                    jsCallback = jsCallback
                )
                HtmlTarget.Parent,
                HtmlTarget.Top,
                HtmlTarget.Self,
                HtmlTarget.Iab -> openBrowser(
                    url = urlString,
                    external = false,
                    jsCallback = jsCallback
                )
            }
        }
    }

    private fun openBrowser(
        url: String,
        external: Boolean,
        jsCallback: (Result<Success<String>, Failure>) -> Unit
    ) {
        when (external) {
            true -> {
                try {
                    val defaultBrowser = Intent(
                        Intent.ACTION_VIEW
                    )
                    defaultBrowser.data = Uri.parse(url)
                    context.startActivity(defaultBrowser)
                    jsCallback(Success("\"$url\""))
                } catch (e: Exception) {
                    jsCallback(
                        Failure(
                            Error(
                                GeotabDriveError.MODULE_GEOLOCATION_ERROR,
                                BrowserModule.ERROR_GETTING_MAPS_APP_IN_DEVICE
                            )
                        )
                    )
                }
            }
            false -> {
                val browserFragment = BrowserFragment.newInstance(url)
                replaceFragment(browserFragment, BrowserModule.BROWSER_TAG, fragmentManager)
                jsCallback(Success("\"$url\""))
            }
        }
    }

    override fun getType(): Type {
        return object : TypeToken<OpenBrowserWindowArgument>() {}.type
    }
}
