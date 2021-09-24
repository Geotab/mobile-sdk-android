package com.geotab.mobile.sdk.module.fileSystem

import android.content.Context
import android.net.Uri
import com.geotab.mobile.sdk.module.Module
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class FileSystemModule(val context: Context) : Module("fileSystem"), CoroutineScope {
    companion object {
        const val fsPrefix = "drvfs"
        const val encoding = "UTF-8"
        const val FILESYSTEM_NOT_EXIST = "Drvfs filesystem doesn\'t exist."
        const val INVALID_FILE_PATH = "Invalid file path - "
        const val FILE_NOT_EXIST = "File doesn\'t exist."
        const val FILE_ALREADY_EXISTS = "File already exists at "
        const val FILE_SIZE_LIMIT = "File size is off limit."
        const val FILE_IS_DIRECTORY = "Given file is a directory"
        const val FILE_IS_NOT_DIRECTORY = "Given file is not a directory"
        const val FOLDER_NOT_EMPTY = "Folder is not empty: "
        const val CREATEDIRECTORY_FAIL = "Can\'t create directory for given path"
        const val WRITEFILE_FAIL = "Can\'t write to file for destined path"
        const val READFILE_FAIL = "Can\'t read from file "
        const val LIST_FAIL = "Can\'t list files from given directory"
        const val GET_FILE_INFO_FAIL = "Can\'t get file info"
        const val DELETEFILE_FAIL = "Can\'t delete file "
        const val MOVE_FILE_FAIL = "Can\'t move file "
        const val DELETE_FOLDER_FAIL = "Failed deleting folder: "
        const val PATH_IS_NOT_FOLDER = "Given path is not a folder: "
        const val INVALID_PATH = "Invalid path: "
        const val CANNOT_DELETE_ROOT_FOLDER = "You can't delete the root folder: "
    }

    private val fsExecutor = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val fsContext: CoroutineScope = CoroutineScope(fsExecutor)

    override val coroutineContext: CoroutineContext
        get() = fsContext.coroutineContext

    var drvfsRootUri: Uri? = null

    init {
        val drvfsRootDirFile = File(context.filesDir, fsPrefix)
        if (!drvfsRootDirFile.exists()) {
            drvfsRootDirFile.mkdirs()
        }
        drvfsRootUri = takeIf {
            drvfsRootDirFile.exists()
        }?.let {
            Uri.Builder()
                .scheme(fsPrefix)
                .path(drvfsRootDirFile.absolutePath)
                .build()
        }
        functions.add(WriteFileAsTextFunction(context, module = this))
        functions.add(WriteFileAsBinaryFunction(context, module = this))
        functions.add(ReadFileAsTextFunction(context, module = this))
        functions.add(ReadFileAsBinaryFunction(context, module = this))
        functions.add(DeleteFileFunction(context, module = this))
        functions.add(DeleteFolderFunction(context, module = this))
        functions.add(MoveFileFunction(context, module = this))
        functions.add(ListFunction(context, module = this))
        functions.add(GetFileInfoFunction(context, module = this))
    }
}
