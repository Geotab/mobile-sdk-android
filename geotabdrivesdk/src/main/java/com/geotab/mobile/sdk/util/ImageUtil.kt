package com.geotab.mobile.sdk.util

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.geotab.mobile.sdk.models.Size
import com.geotab.mobile.sdk.module.camera.CameraModule
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Helper class to evaluate image files
 *
 */
class ImageUtil(val context: Context) {
    companion object {
        const val TAG = "ImageUtil"
        const val MAX_WIDTH = 1000
        const val MAX_HEIGHT = 1000
        private const val MINIMUM_POSSIBLE_SCALE = 1.0
    }

    /**
     * Maintain the aspect ratio so the resulting image does not look smooshed
     */
    private fun calculateAspectRatio(
        options: BitmapFactory.Options,
        reqSize: Size
    ): Pair<Float, Float> {
        val (orgHeight: Int, orgWidth: Int) = options.run { outHeight to outWidth }
        var newWidth = reqSize.width.toFloat()
        var newHeight = reqSize.height.toFloat()
        val orgRatio = orgWidth / orgHeight.toDouble()
        val reqRatio = reqSize.width / reqSize.height.toDouble()

        if (orgRatio > reqRatio) {
            newHeight = (newWidth * orgHeight) / orgWidth
        } else if (orgRatio < reqRatio) {
            newWidth = (newHeight * orgWidth) / orgHeight
        }

        return Pair(newWidth.coerceAtLeast(MINIMUM_POSSIBLE_SCALE.toFloat()), newHeight.coerceAtLeast(MINIMUM_POSSIBLE_SCALE.toFloat()))
    }

    /**
     * Rotate Image
     * @param source image source as bitmap
     * @param angle angle to rotate
     * @return rotated image
     * @throws IllegalArgumentException if source has been recycled
     */

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun rotateAndScaleImage(srcUri: Uri, size: Size?, rotation: Float, scaledUri: Uri): Uri? {
        val (_, originalBitmapOptions) = getOriginalImageSize(
            srcUri = srcUri,
            inJustDecodeBounds = true
        )
        var (bitmap, _) = getOriginalImageSize(
            srcUri = srcUri,
            inJustDecodeBounds = false
        )

        try {
            if (bitmap != null) {
                val scaleFactor = calculateScaleFactor(bitmap, size)

                if (rotation != 0F) {
                    bitmap = rotateImage(bitmap, rotation)

                    if (rotation == 90F || rotation == 270F) {
                        // If image is rotated 90 or 270 the height and width need to be flipped,
                        // in order to be scaled correctly
                        originalBitmapOptions.outHeight = originalBitmapOptions.outWidth.also {
                            originalBitmapOptions.outWidth = originalBitmapOptions.outHeight
                        }
                    }
                }

                bitmap = if (size != null) {
                    val aspectWidthHeight = calculateAspectRatio(originalBitmapOptions, size)

                    Bitmap.createScaledBitmap(
                        bitmap,
                        (aspectWidthHeight.first * scaleFactor).toInt(),
                        (aspectWidthHeight.second * scaleFactor).toInt(),
                        true
                    )
                } else {
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scaleFactor).toInt(),
                        (bitmap.height * scaleFactor).toInt(),
                        true
                    )
                }

                val imageCompressionQualityInPercentage = 80

                context.contentResolver.openOutputStream(scaledUri)?.let { oStream ->
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        imageCompressionQualityInPercentage,
                        oStream
                    )
                    oStream.close()
                }
            } else {
                return null
            }

            return scaledUri
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            bitmap?.recycle()
        }
    }

    private fun calculateScaleFactor(bitmap: Bitmap, size: Size?): Float {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)

        val availableMemory = memoryInfo.availMem
        val bitmapSize = bitmap.byteCount

        val memoryThreshold = 0.8 // 80% of the available memory

        val maxAllowedBitmapSize = (availableMemory * memoryThreshold).toInt() //

        var scaleFactor = if (bitmapSize > maxAllowedBitmapSize) {
            sqrt((maxAllowedBitmapSize.toDouble() / bitmapSize.toDouble()))
        } else {
            1.0 // 100% of the bitmap size
        }

        size?.let {
            val widthScale = it.width.toDouble() / bitmap.width
            val heightScale = it.height.toDouble() / bitmap.height
            val targetScale = min(widthScale, heightScale)

            scaleFactor = max(MINIMUM_POSSIBLE_SCALE, min(scaleFactor, targetScale))
        }

        return scaleFactor.toFloat()
    }

    /**
     * Create a new file for Image storing
     * @return File
     */

    fun createImageFile(rootUri: Uri, fileName: String? = null): File {

        val fName: String = fileName?.let {
            "${rootUri.path}/" + it + CameraModule.ENCODING_TYPE_EXT
        }
            ?: (
                "${rootUri.path}/" + SimpleDateFormat(
                    CameraModule.FILENAME_FORMAT,
                    Locale.US
                ).format(Date()) + CameraModule.ENCODING_TYPE_EXT
                )

        return File(fName)
    }

    fun toDrvfsUri(fsRootUri: Uri, destUri: Uri): String? {
        val fsPath = fsRootUri.path
        val destPath = destUri.path

        fsPath?.let {
            if (destPath?.startsWith(fsPath) == true) {
                return fsRootUri.scheme + "://" + destPath.substring(fsPath.length)
            }
        }
        return null
    }

    /**
     * Gets the rotation of an Image
     * @param source the Uri for the image
     * @return Image Orientation as an Float
     */

    fun getImageOrientation(source: Uri): Float {
        val orientation = try {
            context.contentResolver.openInputStream(source).use { inputStream ->
                inputStream?.let {
                    val exif = ExifInterface(it)
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
            }
        } catch (e: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90F
            ExifInterface.ORIENTATION_ROTATE_180 -> 180F
            ExifInterface.ORIENTATION_ROTATE_270 -> 270F
            ExifInterface.ORIENTATION_NORMAL -> 0F
            else -> 0F
        }
    }

    /**
     * Return bitmap options and bitmap for the given image
     * @param srcUri Uri
     * @return BitmapFactory.Options
     */
    private fun getOriginalImageSize(
        srcUri: Uri,
        inJustDecodeBounds: Boolean
    ): Pair<Bitmap?, BitmapFactory.Options> {
        val options = BitmapFactory.Options().apply {
            this.inJustDecodeBounds = inJustDecodeBounds
        }
        var fileStream: InputStream? = null
        val bitmap: Bitmap?
        try {
            fileStream = getInputStreamFromUriString(srcUri.toString())
            bitmap = BitmapFactory.decodeStream(fileStream, null, options)
        } catch (e: Exception) {
            e.printStackTrace()
            return null to options
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Exception while closing file input stream in getOriginalImageSize.")
                }
            }
        }

        return bitmap to options
    }

    private fun getInputStreamFromUriString(uriString: String): InputStream? {
        val uri = Uri.parse(uriString)
        return context.contentResolver.openInputStream(uri)
    }
}
