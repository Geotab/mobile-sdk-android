package com.geotab.mobile.sdk

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.geotab.mobile.sdk.permission.Permission
import com.geotab.mobile.sdk.permission.PermissionDelegate
import com.geotab.mobile.sdk.permission.PermissionHelper
import java.io.File
import java.io.FileOutputStream

class DownloadFiles(
    private val evaluate: (String, (String) -> Unit) -> Unit,
    val context: Context,
    permissionDelegate: PermissionDelegate
) : DownloadListener {

    companion object {
        const val interfaceName = "DownloadHelper"
    }
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
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val downloadUri = url?.toUri()
            if (url?.startsWith("blob:https") == true) {
                blobToBase64(url, mimeType, fileName)
                return
            }
            val request = DownloadManager.Request(downloadUri)
            request.setMimeType(mimeType)
            request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Downloading file...")
            request.setTitle(fileName)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(
                context,
                context.getString(R.string.downloading, fileName),
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.download_error), Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun blobToBase64(blobUrl: String, mimeType: String?, fileName: String?) {
        val script = """
        (async () => {
           var xhr = new XMLHttpRequest();
           xhr.open('GET', '$blobUrl', true);
           xhr.setRequestHeader('Content-type', '$mimeType');
           xhr.responseType = 'blob';
           xhr.onload = () => {
            if (xhr.status === 200) {
                const blobResponse = xhr.response;
                const fileReader = new FileReader();
                fileReader.readAsDataURL(blobResponse);
                fileReader.onloadend = () => {
                    const base64Data = fileReader.result;
                    DownloadHelper.processBase64Data(base64Data, '$mimeType', '$fileName');
                }
            }
            };
            xhr.send();
            })();
            """.trimMargin()
        evaluate(script) { }
    }

    @Suppress("unused")
    @JavascriptInterface
    fun processBase64Data(dataUrl: String, mimeType: String, fileName: String) {
        val downloadFile = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ).toString() + "/" + fileName
        )
        val bytes = dataUrl.split("base64,")[1]
        val base64ToBytes = Base64.decode(bytes, Base64.DEFAULT)
        val os = FileOutputStream(downloadFile, false)
        os.write(base64ToBytes)
        os.flush()
        openDownload(downloadFile, mimeType)
    }

    private fun openDownload(downloadFile: File, mimeType: String) {
        if (downloadFile.exists()) {
            val downloadFileView = Intent(
                Intent.ACTION_VIEW
            )
            val fileURI = FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                downloadFile
            )
            downloadFileView.setDataAndType(fileURI, mimeType)
            downloadFileView.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(downloadFileView)
        }
    }
}
