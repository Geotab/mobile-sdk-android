package com.geotab.mobile.sdk.util

import android.content.Context
import android.net.Uri
import com.geotab.mobile.sdk.Error
import com.geotab.mobile.sdk.models.enums.GeotabDriveError
import com.geotab.mobile.sdk.module.fileSystem.FileInfo
import com.geotab.mobile.sdk.module.fileSystem.FileSystemModule
import com.geotab.mobile.sdk.module.fileSystem.FileSystemModule.Companion.READFILE_FAIL
import com.geotab.mobile.sdk.module.fileSystem.FileSystemModule.Companion.WRITEFILE_FAIL
import java.io.File
import java.io.RandomAccessFile
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

class FileUtils(val context: Context, private val uri: UriWrapper? = null) {
    fun validatePath(uriString: String, isFile: Boolean = true): String? {

        val pathUri = uri?.parse(uriString) ?: Uri.parse(uriString)

        val path = pathUri.path ?: return null

        when {
            pathUri.scheme != FileSystemModule.fsPrefix -> return null

            path.isEmpty() -> return null

            !validateVirtualPath(pathUri.pathSegments, isFile) -> return null
        }

        return path
    }

    fun validatePath(uriString: String, isFile: Boolean = true, rootUri: Uri): File? {

        val pathUri = uri?.parse(uriString) ?: Uri.parse(uriString)

        val path = pathUri.path ?: return null

        when {
            pathUri.scheme != FileSystemModule.fsPrefix -> return null

            path.isEmpty() -> return null

            !validateVirtualPath(pathUri.pathSegments, isFile) -> return null
        }

        var resultFile: File? = null

        relativePathFromString(rootUri, path).path?.let {
            resultFile = File(it)
        }

        return resultFile
    }

    fun getUriPath(pathStr: String): String? {
        val pathUri = uri?.parse(pathStr) ?: Uri.parse(pathStr)

        return pathUri.path
    }

    /**
     * This method consists in validate if the virtual path does not access
     * the parent of the base folder from the application. In other words,
     * the path should not go up more than the base folder.
     *
     * @param pathSegments List of folders names within the path
     * @return if the path does not go up more than the base folder,
     * it will return true. If the path tries to go up more than
     * the base folder, it will return false.
     */
    private fun validateVirtualPath(pathSegments: MutableList<String>, isFile: Boolean): Boolean {
        var folderCount = 0
        val parentFolderNavigation = ".."

        val pathSegmentsResult = if (isFile) {
            // the reason behind this is because in the
            // pathSegments it also brings the name of the file in the
            // last position. In here, we are just removing the file
            pathSegments.dropLast(1).toMutableList()
        } else {
            pathSegments
        }

        pathSegmentsResult.forEach { segment ->
            when (segment) {
                parentFolderNavigation -> {
                    folderCount--

                    if (folderCount < 0) {
                        return false
                    }
                }

                else -> folderCount++
            }
        }

        return folderCount >= 0
    }

    fun validateUrlAndReturnPath(uriString: String): String? {
        var path = validatePath(uriString) ?: return null
        // it's a directory
        if ((path[path.length - 1] == '/')) return null

        // path starts with "/", remove the first characters
        path = path.trimStart('/')

        return path
    }

    fun relativePathFromString(rootUri: Uri, encodedPath: String): Uri {
        return rootUri.buildUpon().appendEncodedPath(encodedPath).build()
    }

    @Throws(Error::class)
    fun writeToFile(
        fileContents: ByteArray,
        path: String,
        rootUri: Uri,
        offset: Long?
    ): Long {

        return try {
            val relativePathUri = relativePathFromString(rootUri, path)
            val randomAccessF = RandomAccessFile(relativePathUri.path, "rw")
            randomAccessF.use {
                offset?.let { randomAccessF.seek(offset) }
                    ?: randomAccessF.seek(randomAccessF.length())
                randomAccessF.write(fileContents, 0, fileContents.size)
                randomAccessF.length()
            }
        } catch (e: Exception) {
            throw Error(GeotabDriveError.FILE_EXCEPTION, "$WRITEFILE_FAIL ${e.message}")
        }
    }

    @Throws(Error::class)
    fun readFile(
        path: String,
        rootUri: Uri,
        offset: Long,
        size: Long?
    ): ByteArray {

        return try {
            val relativePathUri = relativePathFromString(rootUri, path)
            val randomAccessF = RandomAccessFile(relativePathUri.path, "r")
            randomAccessF.use {
                randomAccessF.seek(offset)
                // Read to the end of file.
                val leftToRead = max((randomAccessF.length() - offset), 0L)
                val sizeToRead = size?.takeIf { it <= leftToRead } ?: leftToRead

                if (sizeToRead >= Int.MAX_VALUE) {
                    throw Exception(FileSystemModule.FILE_SIZE_LIMIT)
                }

                val readArray = ByteArray(sizeToRead.toInt())
                randomAccessF.read(readArray, 0, sizeToRead.toInt())
                readArray
            }
        } catch (e: Exception) {
            throw Error(GeotabDriveError.FILE_EXCEPTION, "$READFILE_FAIL:${e.message}")
        }
    }

    /**
     * Converting signed bytearray to unsigned IntArray
     * @param signedArray ByteArray
     */
    fun byteArraytoIntArray(signedArray: ByteArray): IntArray {
        val unsignedArr = IntArray(signedArray.size)
        for (i in signedArray.indices) {
            unsignedArr[i] = signedArray[i].toInt() and 0XFF
        }
        return unsignedArr
    }

    fun isExist(path: String, rootUri: Uri): Boolean {
        relativePathFromString(rootUri, path).path?.let {
            return File(it).exists()
        }
        return false
    }

    fun getFile(path: String, rootUri: Uri): File? {
        relativePathFromString(rootUri, path).path?.let {
            val file = File(it)
            if (file.exists()) {
                return file
            }
        }
        return null
    }

    fun getOrCreateFile(path: String, rootUri: Uri): File? {
        relativePathFromString(rootUri, path).path?.let {
            return File(it)
        } ?: return null
    }

    fun isDirectory(path: String, rootUri: Uri): Boolean {
        relativePathFromString(rootUri, path).path?.let {
            val file = File(it)
            if (file.isDirectory) {
                return true
            }
        }
        return false
    }

    fun isFolderEmpty(file: File?): Boolean {
        return file?.listFiles()?.isEmpty() == true
    }

    fun createDirectory(path: String, rootUri: Uri): Boolean {
        // path without  "/", the value between first index to the last index of "/" will be the directory
        if (path.lastIndexOf("/") > 0) {
            path.substring(0, path.lastIndexOf("/"))
                .takeIf { it.isNotEmpty() }
                ?.also { directory ->
                    relativePathFromString(rootUri, directory).path?.let {
                        if (!File(it).exists() && !File(it).mkdirs()) {
                            return false
                        }
                    }
                }
        }
        return true
    }

    fun getFiles(dirFile: File): List<FileInfo> {
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val fileList = mutableListOf<FileInfo>()
        dirFile.walk().maxDepth(1)
            .filter { p -> p != dirFile }
            .forEach {
                val fileInfo = FileInfo(if (it.isDirectory) null else it.length(), it.name, it.isDirectory, dateFormat.format(it.lastModified()))
                fileList.add(fileInfo)
            }
        return fileList
    }

    fun getFileInfo(dirFile: File): FileInfo {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return FileInfo(
            if (dirFile.isDirectory) null else dirFile.length(),
            getFileNameFromFile(dirFile),
            dirFile.isDirectory,
            dateFormat.format(dirFile.lastModified())
        )
    }

    private fun getFileNameFromFile(file: File): String {

        return if (!file.isDirectory) {
            file.name
        } else {
            val url = file.absoluteFile.toURI().toURL()
            // remove last / from the url
            val slashRemoved = url.path.dropLast(1)
            return if (slashRemoved.substring(slashRemoved.lastIndexOf('/') + 1) == "drvfs") "" else slashRemoved.substring(
                slashRemoved.lastIndexOf('/') + 1
            )
        }
    }

    fun deleteApplicationCacheDir(): Boolean {
        return File(context.cacheDir.path).deleteRecursively()
    }

    fun deleteServiceWorkerDir(): Boolean {
        return File("${context.dataDir.path}/app_webview/Default/Service Worker/").deleteRecursively()
    }
}
