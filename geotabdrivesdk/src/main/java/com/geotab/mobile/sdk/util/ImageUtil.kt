package com.geotab.mobile.sdk.util

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
import androidx.core.graphics.scale
import androidx.core.net.toUri

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
     * Figure out what ratio we can load our image into memory at while still being bigger than
     * our desired width and height
     */
    private fun calculateSampleSize(
        options: BitmapFactory.Options,
        reqSize: Pair<Float, Float>
    ): Float {
        val (reqWidth: Float, reqHeight: Float) = reqSize
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        return if (width.toFloat() / height.toFloat() > reqWidth / reqHeight) {
            width.toFloat() / reqWidth
        } else {
            height.toFloat() / reqHeight
        }
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
            inSampleSize = 1,
            inJustDecodeBounds = true
        )
        size?.let { imgSize ->
            val aspectWidthHeight = calculateAspectRatio(originalBitmapOptions, imgSize)
            originalBitmapOptions.inSampleSize =
                calculateSampleSize(originalBitmapOptions, aspectWidthHeight).toInt()
        }
        val sampleSize = originalBitmapOptions.inSampleSize

        var (bitmap, _) = getOriginalImageSize(
            srcUri = srcUri,
            inSampleSize = sampleSize,
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

                if (size != null) {
                    val aspectWidthHeight = calculateAspectRatio(originalBitmapOptions, size)
                    println("calculate aspect ratio: $aspectWidthHeight")
                    bitmap = bitmap.scale(
                        (aspectWidthHeight.first * scaleFactor).toInt(),
                        (aspectWidthHeight.second * scaleFactor).toInt()
                    )
                    copyBitmapToUri(scaledUri, bitmap)
                } else if (scaleFactor == 1.0F && rotation == 0F) {
                    // Converting an original image to a bitmap format typically results in a size increase of 4 to 5 times.
                    // Decompressing the bitmap is always bigger than the original file
                    // So use the original file, when the rotation is not required and scale factor is 1.0F.
                    // Also, when the size is null and there is enough memory, resulting in scaling factor of 1.0.
                    copyImageFromUri(srcUri, scaledUri)
                    return scaledUri
                } else {
                    bitmap = bitmap.scale(
                        (bitmap.width * scaleFactor).toInt(),
                        (bitmap.height * scaleFactor).toInt()
                    )
                    copyBitmapToUri(scaledUri, bitmap)
                }
            } else {
                // when the bitmap is null
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

    private fun copyBitmapToUri(scaledUri: Uri, bitmap: Bitmap) {
        val imageCompressionQualityInPercentage = 80
        context.contentResolver.openOutputStream(scaledUri)?.let { oStream ->
            bitmap.compress(
                Bitmap.CompressFormat.PNG,
                imageCompressionQualityInPercentage,
                oStream
            )
            oStream.close()
        }
    }

    private fun copyImageFromUri(fromUri: Uri, toUri: Uri) {
        val buffer = ByteArray(8 * 1024)
        try {
            context.contentResolver.openOutputStream(toUri)?.use { oStream ->
                context.contentResolver.openInputStream(fromUri)?.use { iStream ->
                    // Transfer bytes from in to out
                    var bytesRead: Int
                    while (iStream.read(buffer).also { bytesRead = it } != -1) {
                        oStream.write(buffer, 0, bytesRead)
                    }
                }
            }
        } finally {
            // Securely clear the buffer after use to prevent sensitive data from lingering in memory.
            buffer.fill(0)
        }
    }

    private fun calculateScaleFactor(bitmap: Bitmap, size: Size?): Float {
        // total memory in the app's Java runtime that is allocated and ready for new objects
        val totalJVMFreeMemory = Runtime.getRuntime().freeMemory()
        val bitmapSize = bitmap.byteCount

        val memoryThreshold = 0.3 // 30% of the available memory

        val maxAllowedBitmapSize = (totalJVMFreeMemory * memoryThreshold).toInt() //

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
        inSampleSize: Int,
        inJustDecodeBounds: Boolean
    ): Pair<Bitmap?, BitmapFactory.Options> {
        val options = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
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
        val uri = uriString.toUri()
        return context.contentResolver.openInputStream(uri)
    }
}
