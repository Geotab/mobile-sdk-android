package com.geotab.mobile.sdk.module.browser

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.geotab.mobile.sdk.BuildConfig
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.R
import com.geotab.mobile.sdk.databinding.ActivityBrowserBinding
import com.geotab.mobile.sdk.logging.BroadcastLogLevel
import com.geotab.mobile.sdk.logging.Logger
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Result
import com.geotab.mobile.sdk.module.Success
import com.geotab.mobile.sdk.module.sso.SSOModule
import com.geotab.mobile.sdk.util.JsonUtil
import com.geotab.mobile.sdk.util.UserAgentUtil
import com.geotab.mobile.sdk.util.regReceiver
import java.net.MalformedURLException
import java.net.URL

class BrowserFragment : Fragment() {
    private lateinit var browserBinding: ActivityBrowserBinding
    private val webView: WebView by lazy { browserBinding.browserWebview }
    private var broadcastReceiver: BroadcastReceiver? = null
    var samlCallback: ((Result<Success<String>, Failure>) -> Unit)? = null
    var url: String? = null
    var script: String? = null
    var isUserAgentKillSwitchOn = false
    private var isCallbackCompleted = false
    private var isDismissed = false
    private var reachedCallbackPage = false
    internal var isBeingReplaced = false

    private val userAgentUtil: UserAgentUtil by lazy {
        UserAgentUtil(requireContext())
    }

    @Keep
    companion object {
        private const val ARG_URL = "url"
        private const val TAG = "BrowserFragment"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param url to load in the webview
         * @return A new instance of BrowserFragment.
         */
        @Keep
        @JvmStatic
        fun newInstance(url: String): BrowserFragment =
            BrowserFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_URL, url)
                }
            }

        // First match wins. reachedCallbackPage is checked before isDismissed/isBeingReplaced
        // because "reached sso.html but token hop didn't finish" is the highest-signal
        // diagnostic, and it should not be masked by a concurrent dismissal or relaunch.
        internal fun samlCallbackReasonOnDestroy(
            isCallbackCompleted: Boolean,
            reachedCallbackPage: Boolean,
            isDismissed: Boolean,
            isBeingReplaced: Boolean
        ): String? = when {
            isCallbackCompleted -> null
            reachedCallbackPage -> SSOModule.SAML_CLOSED_AFTER_CALLBACK
            isBeingReplaced -> SSOModule.SAML_LAUNCHING_BROWSER
            isDismissed -> SSOModule.SAML_DISMISSED
            else -> SSOModule.SAML_UNEXPECTED_CLOSE
        }

        // SAML_UNEXPECTED_CLOSE needs a native Sentry event. It fires when the host
        // (activity/process) is torn down, which also destroys the Drive WebView that the
        // samlCallback -> evaluate bridge depends on, so the web Sentry pipeline can't
        // receive it.
        internal fun shouldCaptureSamlCloseNatively(reason: String): Boolean =
            reason == SSOModule.SAML_UNEXPECTED_CLOSE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle -> url = bundle.getString(ARG_URL) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, intent: Intent?) {
                when (intent?.action) {
                    "close" -> {
                        isDismissed = true
                        closeFragment()
                    }
                }
            }
        }
        context?.regReceiver(
            broadcastReceiver = broadcastReceiver,
            intentFilter = IntentFilter("close")
        )
        browserBinding = ActivityBrowserBinding.inflate(inflater)
        return browserBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureWebView()
        configureWebViewScript()

        setupToolBar()

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    isDismissed = true
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()

        if (isCallbackCompleted) {
            closeFragment()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        with(this.webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportMultipleWindows(true)
            userAgentString = if (isUserAgentKillSwitchOn) {
                userAgentUtil.getUserAgent(webView.settings.userAgentString)
            } else {
                Logger.shared.debug(
                    TAG,
                    "UserAgent: ${userAgentUtil.getUserAgent(webView.settings.userAgentString)}"
                )
                userAgentUtil.getUserAgent(webView.settings.userAgentString).replace("; wv", "")
            }
            setGeolocationEnabled(true)
        }
    }

    private fun configureWebViewScript() {
        url?.let {
            webView.loadUrl(it)
        }
        webView.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (view != null && view.title != null) {
                    browserBinding.toolBar.title = view.title
                }
                if (url?.contains("sso.html", true) != true) {
                    return
                }
                reachedCallbackPage = true
                view?.let { webView ->
                    if (script != null) {
                        webView.evaluateJavascript(script!!) {
                            val result = JsonUtil.toJson(it)
                            onSuccess(result)
                        }
                    }
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (error?.errorCode == ERROR_HOST_LOOKUP && request?.isForMainFrame == true) {
                    onErrorAndClose(error.description.toString())
                }
                super.onReceivedError(view, request, error)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        broadcastReceiver?.let {
            context?.unregisterReceiver(broadcastReceiver)
        }
        broadcastReceiver = null
        webView.clearHistory()
        webView.clearCache(true)
        webView.destroy()

        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies {}

        val samlCloseReason = samlCallbackReasonOnDestroy(
            isCallbackCompleted = isCallbackCompleted,
            reachedCallbackPage = reachedCallbackPage,
            isDismissed = isDismissed,
            isBeingReplaced = isBeingReplaced
        )
        samlCloseReason?.let { reason ->
            if (shouldCaptureSamlCloseNatively(reason)) {
                Logger.shared.event(
                    level = BroadcastLogLevel.ERROR,
                    tag = TAG,
                    message = reason,
                    exception = Error(GeotabDriveError.MODULE_SAML_ERROR, reason),
                    tags = mapOf("saml_close_reason" to reason)
                )
            }
            onError(reason)
        }
    }

    private fun setupToolBar() {
        val toolBar = browserBinding.toolBar
        url?.let { urlString ->
            toolBar.title = try {
                URL(urlString).host
            } catch (e: MalformedURLException) {
                urlString
            }
        }
        toolBar.navigationIcon = context?.let {
            ContextCompat.getDrawable(it, R.drawable.ic_baseline_close_24)
        }
        toolBar.setNavigationOnClickListener {
            isDismissed = true
            closeFragment()
        }
    }

    fun onSuccess(token: String) {
        samlCallback?.invoke(Success(token))
        isCallbackCompleted = true
        closeFragment()
    }

    fun onErrorAndClose(error: String) {
        samlCallback?.let {
            it.invoke(Failure(Error(GeotabDriveError.MODULE_SAML_ERROR, error)))
            isCallbackCompleted = true
            closeFragment()
        }
    }

    private fun onError(error: String) {
        samlCallback?.invoke(Failure(Error(GeotabDriveError.MODULE_SAML_ERROR, error)))
    }

    internal fun closeFragment() {
        if (!parentFragmentManager.isStateSaved) {
            parentFragmentManager.popBackStack()
        }
    }
}
