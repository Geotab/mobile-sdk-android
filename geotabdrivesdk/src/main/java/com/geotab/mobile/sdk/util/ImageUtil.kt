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
import kotlin.math.ceil

/**
 * Helper class to evaluate image files
 *
 */
class ImageUtil(val context: Context) {
    companion object {
        const val TAG = "ImageUtil"
        const val MAX_WIDTH = 1000
        const val MAX_HEIGHT = 1000
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
        return Pair(newWidth, newHeight)
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
     *  Returns bitmap for the given image Uri
     * @param srcUri Uri
     * @param options Options
     * @return Bitmap?
     */
    private fun getBitmap(srcUri: Uri, options: BitmapFactory.Options): Bitmap? {
        val bitmap: Bitmap?
        var fileStream: InputStream? = null
        try {
            fileStream = getInputStreamFromUriString(srcUri.toString())
            bitmap = BitmapFactory.decodeStream(fileStream, null, options)
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Exception while closing file input stream in getBitmap.")
                }
            }
        }
        return bitmap
    }

    /**
     * Return bitmap options for the given image
     * @param srcUri Uri
     * @return BitmapFactory.Options
     */
    private fun getOriginalImageSize(srcUri: Uri): BitmapFactory.Options {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        var fileStream: InputStream? = null
        try {
            fileStream = getInputStreamFromUriString(srcUri.toString())
            BitmapFactory.decodeStream(fileStream, null, options)
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Exception while closing file input stream in getOriginalImageSize.")
                }
            }
        }
        return options
    }

    private fun getInputStreamFromUriString(uriString: String): InputStream? {
        val uri = Uri.parse(uriString)
        return context.contentResolver.openInputStream(uri)
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

        val imageOptions = getOriginalImageSize(srcUri)
        imageOptions.inJustDecodeBounds = false

        var manipulatedBitmap: Bitmap? = getBitmap(srcUri, imageOptions)

        try {

            if (rotation != 0F) {
                manipulatedBitmap?.let {
                    manipulatedBitmap = rotateImage(it, rotation)
                }
                if (rotation == 90F || rotation == 270F) {
                    // If image is rotated 90 or 270 the height and width need to be flipped,
                    // in order to be scaled correctly
                    imageOptions.outHeight = imageOptions.outWidth.also {
                        imageOptions.outWidth = imageOptions.outHeight
                    }
                }
            }

            size?.let { imgSize ->
                val aspectWidthHeight = calculateAspectRatio(imageOptions, imgSize)
                imageOptions.inSampleSize =
                    calculateSampleSize(imageOptions, aspectWidthHeight).toInt()
                manipulatedBitmap?.let {
                    manipulatedBitmap = Bitmap.createScaledBitmap(
                        it,
                        ceil(aspectWidthHeight.first).toInt(),
                        ceil(aspectWidthHeight.second).toInt(),
                        true
                    )
                }
            }

            manipulatedBitmap?.let {
                context.contentResolver.openOutputStream(scaledUri)?.let { oStream ->
                    it.compress(Bitmap.CompressFormat.PNG, 100, oStream)
                    oStream.close()
                }
            } ?: return null

            return scaledUri
        } finally {
            manipulatedBitmap?.recycle()
        }
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
}
