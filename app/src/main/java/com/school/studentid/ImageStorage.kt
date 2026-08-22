package com.school.studentid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/**
 * Handles saving student photos (from camera or gallery) permanently inside
 * the app's own storage, while automatically optimizing them:
 *  - resized so the longest side is ~800px (keeps aspect ratio)
 *  - compressed as JPEG at ~78% quality
 *  - correct orientation applied (using EXIF), so photos never appear
 *    sideways/upside-down after optimizing
 *  - decoded memory-efficiently (downsampled while reading) so large camera
 *    photos never risk an OutOfMemoryError
 */
object ImageStorage {
    private const val FOLDER_NAME = "student_photos"
    private const val MAX_DIMENSION = 800   // longest side, in pixels
    private const val JPEG_QUALITY = 78     // 0-100

    private fun photosDir(context: Context): File =
        File(context.filesDir, FOLDER_NAME).apply { mkdirs() }

    /**
     * Copies a picked gallery image into permanent app storage, resizing and
     * compressing it along the way. Returns the new (optimized) file's
     * absolute path, or null if something went wrong.
     */
    fun copyToAppStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val orientation = readOrientationFromUri(context, sourceUri)
            val rawBitmap = decodeSampledBitmapFromUri(context, sourceUri, MAX_DIMENSION) ?: return null
            saveOptimizedBitmap(context, rawBitmap, orientation)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Creates a brand-new empty file (in permanent app storage) for the
     * camera to write its full-resolution capture into. This raw file gets
     * replaced by [optimizeCapturedPhoto] right after the camera returns.
     */
    fun newCameraOutputFile(context: Context): File =
        File(photosDir(context), "photo_${System.currentTimeMillis()}_raw.jpg")

    /**
     * Takes a freshly captured, full-resolution camera photo and replaces it
     * with a resized + compressed version (correct orientation applied),
     * deleting the large original. Returns the optimized file's path.
     */
    fun optimizeCapturedPhoto(context: Context, capturedFile: File): String? {
        return try {
            val orientation = readOrientationFromFile(capturedFile)
            val rawBitmap = decodeSampledBitmapFromFile(capturedFile, MAX_DIMENSION) ?: return null
            val savedPath = saveOptimizedBitmap(context, rawBitmap, orientation)
            capturedFile.delete() // remove the large, unoptimized original
            savedPath
        } catch (e: Exception) {
            null
        }
    }

    // ---------------- internal helpers ----------------

    private fun saveOptimizedBitmap(context: Context, rawBitmap: Bitmap, orientation: Int): String? {
        val rotated = applyOrientation(rawBitmap, orientation)
        val resized = scaleToMaxDimension(rotated, MAX_DIMENSION)

        val destFile = File(photosDir(context), "photo_${System.currentTimeMillis()}.jpg")
        FileOutputStream(destFile).use { out ->
            resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }

        if (resized !== rotated) resized.recycle()
        if (rotated !== rawBitmap) rotated.recycle()
        rawBitmap.recycle()

        return destFile.absolutePath
    }

    private fun readOrientationFromUri(context: Context, uri: Uri): Int = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    private fun readOrientationFromFile(file: File): Int = try {
        ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleToMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longestSide = maxOf(bitmap.width, bitmap.height)
        if (longestSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / longestSide
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

        options.inSampleSize = calculateInSampleSize(options, reqSize, reqSize)
        options.inJustDecodeBounds = false

        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }

    private fun decodeSampledBitmapFromFile(file: File, reqSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)

        options.inSampleSize = calculateInSampleSize(options, reqSize, reqSize)
        options.inJustDecodeBounds = false

        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
