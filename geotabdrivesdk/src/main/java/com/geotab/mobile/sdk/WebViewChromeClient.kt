package com.geotab.mobile.sdk

import android.os.Message
import android.webkit.GeolocationPermissions
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.geotab.mobile.sdk.permission.Permission
import com.geotab.mobile.sdk.permission.PermissionHelper

class WebViewChromeClient(
    private val permissionHelper: PermissionHelper? = null
) : WebChromeClient() {

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        if (!isUserGesture) return false
        if (view == null || resultMsg == null) return false

        val newView = WebView(view.context)
        val transport = resultMsg.obj as WebView.WebViewTransport
        transport.webView = newView
        resultMsg.sendToTarget()
        return true
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        return super.onJsAlert(view, url, message, result)
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        return super.onJsConfirm(view, url, message, result)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        super.onGeolocationPermissionsShowPrompt(origin, callback)

        permissionHelper?.let {
            it.checkPermission(arrayOf(Permission.LOCATION)) { hasLocationPermission ->
                callback?.invoke(
                    origin,
                    hasLocationPermission,
                    false
                )
            }
        }
    }
}
