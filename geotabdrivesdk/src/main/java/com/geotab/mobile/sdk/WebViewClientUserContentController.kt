package com.geotab.mobile.sdk

import android.annotation.SuppressLint
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.geotab.mobile.sdk.models.interfaces.WebViewClientController
import com.geotab.mobile.sdk.module.NetworkErrorDelegate

class WebViewClientUserContentController(private val networkErrorDelegate: NetworkErrorDelegate) :
    WebViewClientController, WebViewClient() {
    private var moduleScripts = ""

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
            webView.evaluateJavascript(moduleScripts) {}
        }
    }
}
