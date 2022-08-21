package com.geotab.mobile.sdk

import android.net.Uri
import android.os.Message
import android.webkit.GeolocationPermissions
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.geotab.mobile.sdk.fileChooser.FileChooserHelper
import com.geotab.mobile.sdk.permission.Permission
import com.geotab.mobile.sdk.permission.PermissionHelper

class WebViewChromeClient(
    private val permissionHelper: PermissionHelper? = null,
    private val fileChooserHelper: FileChooserHelper? = null
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

    override fun onPermissionRequest(request: PermissionRequest?) {
        request?.resources?.forEach { resource ->
            if (resource.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                permissionHelper?.checkPermission(arrayOf(Permission.CAMERA)) { hasCameraPermission ->
                    if (hasCameraPermission) {
                        request.grant(arrayOf(resource))
                    } else {
                        request.deny()
                    }
                }
            } else {
                super.onPermissionRequest(request)
            }
        }
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        super.onGeolocationPermissionsShowPrompt(origin, callback)

        val permissions = permissionHelper?.getLocationPermissionsBasedOnAndroidApi()

        permissions?.let { arrayOfPermissions ->
            permissionHelper?.checkPermission(arrayOfPermissions) { hasLocationPermission ->
                callback?.invoke(
                    origin,
                    hasLocationPermission,
                    false
                )
            }
        }
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        fileChooserHelper?.chooseFile(filePathCallback, fileChooserParams)
        return true
    }
}
