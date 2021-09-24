package com.geotab.mobile.sdk

import android.content.Context
import android.os.Message
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.geotab.mobile.sdk.permission.Permission
import com.geotab.mobile.sdk.permission.PermissionDelegate
import com.geotab.mobile.sdk.permission.PermissionHelper

class WebViewChromeClient(
    private val context: Context,
    private val permissionDelegate: PermissionDelegate
) : WebChromeClient() {
    private val permissionHelper: PermissionHelper by lazy {
        PermissionHelper(context, permissionDelegate)
    }

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

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        super.onGeolocationPermissionsShowPrompt(origin, callback)

        permissionHelper.checkPermission(arrayOf(Permission.LOCATION)) { hasLocationPermission ->
            callback?.invoke(
                origin,
                hasLocationPermission,
                false
            )
        }
    }
}
