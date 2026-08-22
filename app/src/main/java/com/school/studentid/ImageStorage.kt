package com.school.studentid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
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
 *  - optionally composited onto a plain White or Blue backdrop (the photo
 *    is scaled to fit, not cropped, so the chosen background shows around it)
 */
object ImageStorage {
    private const val FOLDER_NAME = "student_photos"
    private const val MAX_DIMENSION = 800   // longest side, in pixels
    private const val JPEG_QUALITY = 78     // 0-100

    private fun photosDir(context: Context): File =
        File(context.filesDir, FOLDER_NAME).apply { mkdirs() }

    /**
     * Processes a picked/captured photo and saves the final, optimized
     * result into permanent app storage. Pass exactly one of [sourceUri]
     * (gallery pick) or [sourceFile] (camera capture — this raw file is
     * deleted afterwards). If [backgroundColor] is null, the photo fills
     * the frame as before ("Original Image"); otherwise the photo is
     * scaled to fit within a plain square of that color (e.g. White/Blue),
     * like a passport-photo backdrop. Returns the saved file's path.
     */
    fun processAndSave(
        context: Context,
        sourceUri: Uri? = null,
        sourceFile: File? = null,
        backgroundColor: Int?
    ): String? {
        return try {
            val orientation = when {
                sourceUri != null -> readOrientationFromUri(context, sourceUri)
                sourceFile != null -> readOrientationFromFile(sourceFile)
                else -> ExifInterface.ORIENTATION_NORMAL
            }
            val rawBitmap = when {
                sourceUri != null -> decodeSampledBitmapFromUri(context, sourceUri, MAX_DIMENSION)
                sourceFile != null -> decodeSampledBitmapFromFile(sourceFile, MAX_DIMENSION)
                else -> null
            } ?: return null

            val rotated = applyOrientation(rawBitmap, orientation)

            val finalBitmap = if (backgroundColor != null) {
                composeOnBackground(rotated, backgroundColor, MAX_DIMENSION)
            } else {
                scaleToMaxDimension(rotated, MAX_DIMENSION)
            }

            val destFile = File(photosDir(context), "photo_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }

            if (finalBitmap !== rotated) finalBitmap.recycle()
            if (rotated !== rawBitmap) rotated.recycle()
            rawBitmap.recycle()

            sourceFile?.delete() // remove the large, unoptimized camera original

            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Creates a brand-new empty file (in permanent app storage) for the camera to write into. */
    fun newCameraOutputFile(context: Context): File =
        File(photosDir(context), "photo_${System.currentTimeMillis()}_raw.jpg")

    // ---------------- internal helpers ----------------

    /** Draws [bitmap] centered and scaled-to-fit (never cropped) onto a plain square of [backgroundColor]. */
    private fun composeOnBackground(bitmap: Bitmap, backgroundColor: Int, canvasSize: Int): Bitmap {
        val canvasBitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)
        canvas.drawColor(backgroundColor)

        val maxContentSize = canvasSize * 0.82f
        val scale = minOf(maxContentSize / bitmap.width, maxContentSize / bitmap.height)
        val destW = (bitmap.width * scale).toInt()
        val destH = (bitmap.height * scale).toInt()
        val left = (canvasSize - destW) / 2
        val top = (canvasSize - destH) / 2
        val destRect = Rect(left, top, left + destW, top + destH)

        val paint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }
        canvas.drawBitmap(bitmap, null, destRect, paint)
        return canvasBitmap
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
