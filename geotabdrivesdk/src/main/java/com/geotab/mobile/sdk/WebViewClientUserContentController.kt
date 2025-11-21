package com.geotab.mobile.sdk

import android.annotation.SuppressLint
import android.content.Intent
import android.net.http.SslError
import android.os.Build
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import com.geotab.mobile.sdk.logging.Logger
import com.geotab.mobile.sdk.models.interfaces.WebViewClientController
import com.geotab.mobile.sdk.module.NetworkErrorDelegate

class WebViewClientUserContentController(private val networkErrorDelegate: NetworkErrorDelegate) :
    WebViewClientController, WebViewClient() {
    companion object {
        const val TAG = "WebViewClientUserContentController"
    }

    private var moduleScripts = ""
    private var appOnBackPressedCallback: OnBackPressedCallback? = null
    private var webViewOnBackPressedCallback: OnBackPressedCallback? = null

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        if (error?.errorCode == ERROR_HOST_LOOKUP && request?.isForMainFrame == true) {
            networkErrorDelegate.onNetworkError()
        }
        super.onReceivedError(view, request, error)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        detail?.let {
            val crashMessage =
                if (it.didCrash()) {
                    "Webview rendering process crashed"
                } else {
                    "System killed the WebView rendering process to reclaim memory."
                }
            Logger.shared.error(
                TAG,
                crashMessage
            )
        }
        return super.onRenderProcessGone(view, detail)
    }

    @SuppressLint("WebViewClientOnReceivedSslError")
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, err: SslError) {
        if (BuildConfig.DEBUG) {
            handler.proceed()
        } else {
            super.onReceivedSslError(view, handler, err)
        }
    }

    override fun addScriptOnPageFinished(script: String) {
        moduleScripts = """if (window.geotabModules == null) { $script }"""
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        view?.let { webView ->
            webView.evaluateJavascript(moduleScripts, null)
            webViewOnBackPressedCallback?.isEnabled = webView.canGoBack()
            appOnBackPressedCallback?.isEnabled = !webView.canGoBack()
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val urlScheme = request?.url?.scheme ?: ""
        if (urlScheme.matches(Regex("(tel|mailto|sms|geo)")) && view?.context != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = request?.url
                view.context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Logger.shared.error(
                    TAG,
                    "Error navigating to URL with scheme $urlScheme from WebView context."
                )
            }
        }
        return false
    }

    fun setWebViewCallBack(onBackPressedCallBack: OnBackPressedCallback?) {
        webViewOnBackPressedCallback = onBackPressedCallBack
    }

    fun setAppCallBack(onBackPressedCallBack: OnBackPressedCallback?) {
        appOnBackPressedCallback = onBackPressedCallBack
    }
}
