package com.geotab.mobile.sdk

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.widget.Toast
import com.geotab.mobile.sdk.permission.Permission
import com.geotab.mobile.sdk.permission.PermissionDelegate
import com.geotab.mobile.sdk.permission.PermissionHelper

class DownloadFiles(val context: Context, permissionDelegate: PermissionDelegate) :
    DownloadListener {
    private val permissionHelper: PermissionHelper by lazy {
        PermissionHelper(context, permissionDelegate)
    }

    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissionHelper.checkPermission(arrayOf(Permission.WRITE_EXTERNAL)) { isSuccess ->
                if (isSuccess) {
                    startDownload(url, userAgent, contentDisposition, mimetype)
                } else {
                    Toast.makeText(context, "Permission denied to download File", Toast.LENGTH_LONG)
                        .show()
                }
            }
        } else {
            startDownload(url, userAgent, contentDisposition, mimetype)
        }
    }

    private fun startDownload(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimeType)
            request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Downloading file...")
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            request.setTitle(fileName)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(context, context.getString(R.string.downloading, fileName), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.download_error), Toast.LENGTH_LONG).show()
        }
    }
}
