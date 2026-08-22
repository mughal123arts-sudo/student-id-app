package com.school.studentid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a single PDF containing one page per student: a simple, plain
 * layout — school name, photo, then values (no colored bands, no card
 * background, no field labels).
 *
 * Uses Android's built-in PdfDocument so no extra library/dependency is
 * needed. Photos are decoded at only the resolution actually needed for
 * display (not full resolution) and in RGB_565 (no alpha channel needed),
 * which keeps both memory use and the final PDF file size small.
 */
object PdfExporter {

    // Roughly A4 proportions at ~150dpi.
    private const val PAGE_WIDTH = 1240
    private const val PAGE_HEIGHT = 1754

    /**
     * @param fileNameHint When provided (e.g. the class folder name), the
     * output file is named "<CleanName>_<dd-MM-yyyy>.pdf" — e.g.
     * "Playgroup_04-08-2026.pdf" or "1_04-08-2026.pdf" for "Class 1" (the
     * word "Class" and any parenthetical suffix are stripped). When null,
     * falls back to a timestamp-based name.
     */
    fun generateStudentsPdf(context: Context, students: List<Student>, fileNameHint: String? = null): File? {
        if (students.isEmpty()) return null

        val schoolName = AppPreferences.getSchoolName(context).ifBlank { "" }

        val document = PdfDocument()
        return try {
            students.forEachIndexed { index, student ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                val page = document.startPage(pageInfo)
                drawStudentPage(page.canvas, student, schoolName, index + 1, students.size)
                document.finishPage(page)
            }

            val exportsDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
            val fileName = if (!fileNameHint.isNullOrBlank()) {
                val cleanName = cleanNameForFileName(fileNameHint)
                val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                "${cleanName}_${dateStr}.pdf"
            } else {
                "students_${System.currentTimeMillis()}.pdf"
            }
            val file = File(exportsDir, fileName)
            FileOutputStream(file).use { out -> document.writeTo(out) }
            file
        } catch (e: Exception) {
            null
        } finally {
            document.close()
        }
    }

    /**
     * Turns a folder name into a clean filename fragment:
     * "Class 1" -> "1", "Playgroup (PG)" -> "Playgroup", "Other Classes" -> "Other_Classes".
     */
    private fun cleanNameForFileName(name: String): String {
        var cleaned = name.replace(Regex("(?i)^class\\s+"), "")
        cleaned = cleaned.replace(Regex("\\s*\\([^)]*\\)"), "")
        return cleaned.trim().replace(" ", "_")
    }

    private fun drawStudentPage(canvas: Canvas, student: Student, schoolName: String, pageNumber: Int, totalPages: Int) {
        val centerX = PAGE_WIDTH / 2f
        var y = 100f

        // ---------------- Plain header (no colored band) ----------------
        if (schoolName.isNotBlank()) {
            val schoolNamePaint = Paint().apply {
                color = Color.BLACK; textSize = 44f; isFakeBoldText = true
                isAntiAlias = true; textAlign = Paint.Align.CENTER
            }
            canvas.drawText(schoolName, centerX, y, schoolNamePaint)
            y += 50f
        }

        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY; textSize = 28f; isAntiAlias = true; textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Student ID Card", centerX, y, subtitlePaint)
        y += 70f

        // ---------------- Photo (small, memory-efficient decode) ----------------
        val photoSize = 380f
        val photoLeft = centerX - photoSize / 2
        val photoTop = y
        val photoRect = RectF(photoLeft, photoTop, photoLeft + photoSize, photoTop + photoSize)

        var photoDrawn = false
        student.photoUri?.let { path ->
            val file = File(path)
            if (file.exists()) {
                // Decode only at the resolution this box actually needs —
                // keeps memory use low and keeps the PDF file small.
                val bitmap = decodeBitmapForPdf(file.absolutePath, photoSize.toInt())
                if (bitmap != null) {
                    val photoPaint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }
                    canvas.drawBitmap(bitmap, null, photoRect, photoPaint)
                    bitmap.recycle()
                    photoDrawn = true
                }
            }
        }

        val photoBorderPaint = Paint().apply { style = Paint.Style.STROKE; color = Color.BLACK; strokeWidth = 2f }
        canvas.drawRect(photoRect, photoBorderPaint)

        if (!photoDrawn) {
            val noPhotoPaint = Paint().apply {
                color = Color.GRAY; textSize = 28f; isAntiAlias = true; textAlign = Paint.Align.CENTER
            }
            canvas.drawText("No Photo", centerX, photoTop + photoSize / 2, noPhotoPaint)
        }

        y = photoTop + photoSize + 60f

        // ---------------- Student name ----------------
        val namePaint = Paint().apply {
            color = Color.BLACK; textSize = 40f; isFakeBoldText = true
            isAntiAlias = true; textAlign = Paint.Align.CENTER
        }
        canvas.drawText(student.studentName.ifBlank { "-" }, centerX, y, namePaint)
        y += 60f

        // ---------------- Remaining details — plain values, no labels, no lines ----------------
        val valuePaint = Paint().apply {
            color = Color.DKGRAY; textSize = 30f; isAntiAlias = true; textAlign = Paint.Align.CENTER
        }

        val values = listOf(
            student.fatherName,
            if (student.section.isNotBlank()) "${student.className} - ${student.section}" else student.className,
            student.rollNumber,
            student.mobileNumber
        )

        values.forEach { value ->
            canvas.drawText(value.ifBlank { "-" }, centerX, y, valuePaint)
            y += 50f
        }

        // ---------------- Notes (wrapped, only if present) ----------------
        if (student.notes.isNotBlank()) {
            y += 15f
            val notesPaint = Paint().apply {
                color = Color.DKGRAY; textSize = 26f; isAntiAlias = true; textAlign = Paint.Align.CENTER
            }
            val maxWidth = PAGE_WIDTH - 200f
            wrapText(student.notes, notesPaint, maxWidth).forEach { line ->
                if (y < PAGE_HEIGHT - 100f) {
                    canvas.drawText(line, centerX, y, notesPaint)
                    y += 36f
                }
            }
        }

        // ---------------- Footer ----------------
        val footerPaint = Paint().apply {
            color = Color.GRAY; textSize = 22f; isAntiAlias = true; textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Page $pageNumber of $totalPages", centerX, PAGE_HEIGHT - 50f, footerPaint)
    }

    /** Simple word-wrap so long Notes text doesn't run off the page. */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()

        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) > maxWidth && current.isNotEmpty()) {
                lines.add(current.toString())
                current = StringBuilder(word)
            } else {
                current = StringBuilder(candidate)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    /**
     * Decodes a photo downsampled to roughly [reqSize] pixels, in RGB_565
     * (photos need no alpha channel) — keeps memory use and the final PDF
     * file size small.
     */
    private fun decodeBitmapForPdf(path: String, reqSize: Int): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, boundsOptions)

        var inSampleSize = 1
        val height = boundsOptions.outHeight
        val width = boundsOptions.outWidth
        if (height > reqSize || width > reqSize) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqSize && (halfWidth / inSampleSize) >= reqSize) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeFile(path, decodeOptions)
    }
}
