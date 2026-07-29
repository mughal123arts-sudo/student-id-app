package com.school.studentid

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Photos picked from the gallery or taken from the camera need to be copied
 * into the app's own storage. Otherwise the original content:// URI can stop
 * working after the app restarts (permission to read it is not permanent),
 * which is why saved photos were disappearing.
 */
object ImageStorage {
    private const val FOLDER_NAME = "student_photos"

    private fun photosDir(context: Context): File =
        File(context.filesDir, FOLDER_NAME).apply { mkdirs() }

    /** Copies a picked gallery image into permanent app storage. Returns the new file's absolute path. */
    fun copyToAppStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val destFile = File(photosDir(context), "photo_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Creates a brand-new empty file (in permanent app storage) for the camera to write into. */
    fun newCameraOutputFile(context: Context): File =
        File(photosDir(context), "photo_${System.currentTimeMillis()}.jpg")
}
