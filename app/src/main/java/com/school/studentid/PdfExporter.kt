package com.school.studentid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

/**
 * Builds a single PDF containing one page per student, styled like a proper
 * ID card: a colored header band, a bordered card panel with a rounded photo
 * box, and clean values below (no field labels).
 *
 * Uses Android's built-in PdfDocument so no extra library/dependency is
 * needed. Photos are decoded at only the resolution actually needed for
 * display (not full resolution) and in RGB_565 (no alpha channel needed),
 * which keeps both memory use and the final PDF file size small.
 */
object PdfExporter {

    // ---- Customize these two to match your school ----
    private const val SCHOOL_NAME = "School Name"
    private val HEADER_COLOR = Color.rgb(21, 63, 130)   // navy blue band
    private val ACCENT_COLOR = Color.rgb(21, 63, 130)   // borders / accents

    // Roughly A4 proportions at ~150dpi.
    private const val PAGE_WIDTH = 1240
    private const val PAGE_HEIGHT = 1754

    fun generateStudentsPdf(context: Context, students: List<Student>): File? {
        if (students.isEmpty()) return null

        val document = PdfDocument()
        return try {
            students.forEachIndexed { index, student ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                val page = document.startPage(pageInfo)
                drawStudentPage(page.canvas, student, index + 1, students.size)
                document.finishPage(page)
            }

            val exportsDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
            val file = File(exportsDir, "students_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out -> document.writeTo(out) }
            file
        } catch (e: Exception) {
            null
        } finally {
            document.close()
        }
    }

    private fun drawStudentPage(canvas: Canvas, student: Student, pageNumber: Int, totalPages: Int) {
        val centerX = PAGE_WIDTH / 2f

        // ---------------- Header band ----------------
        val headerHeight = 190f
        val headerPaint = Paint().apply { color = HEADER_COLOR }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight, headerPaint)

        val schoolNamePaint = Paint().apply {
            color = Color.WHITE; textSize = 50f; isFakeBoldText = true
            isAntiAlias = true; textAlign = Paint.Align.CENTER
        }
        val subtitlePaint = Paint().apply {
            color = Color.WHITE; textSize = 30f; isAntiAlias = true; textAlign = Paint.Align.CENTER
        }
        canvas.drawText(SCHOOL_NAME, centerX, 90f, schoolNamePaint)
        canvas.drawText("STUDENT ID CARD", centerX, 140f, subtitlePaint)

        // ---------------- Card panel ----------------
        val cardLeft = 100f
        val cardRight = PAGE_WIDTH - 100f
        val cardTop = headerHeight + 60f
        val cardBottom = PAGE_HEIGHT - 140f
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)

        val cardBgPaint = Paint().apply { color = Color.rgb(247, 249, 252) }
        val cardBorderPaint = Paint().apply { style = Paint.Style.STROKE; color = ACCENT_COLOR; strokeWidth = 3f }
        canvas.drawRoundRect(cardRect, 24f, 24f, cardBgPaint)
        canvas.drawRoundRect(cardRect, 24f, 24f, cardBorderPaint)

        var y = cardTop + 90f

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
                    val clipPath = Path().apply { addRoundRect(photoRect, 20f, 20f, Path.Direction.CW) }
                    canvas.save()
                    canvas.clipPath(clipPath)
                    val photoPaint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }
                    canvas.drawBitmap(bitmap, null, photoRect, photoPaint)
                    canvas.restore()
                    bitmap.recycle()
                    photoDrawn = true
                }
            }
        }

        val photoBorderPaint = Paint().apply { style = Paint.Style.STROKE; color = ACCENT_COLOR; strokeWidth = 4f }
        canvas.drawRoundRect(photoRect, 20f, 20f, photoBorderPaint)

        if (!photoDrawn) {
            val noPhotoPaint = Paint().apply {
                color = Color.GRAY; textSize = 28f; isAntiAlias = true; textAlign = Paint.Align.CENTER
            }
            canvas.drawText("No Photo", centerX, photoTop + photoSize / 2, noPhotoPaint)
        }

        y = photoTop + photoSize + 60f

        // ---------------- Student name ----------------
        val namePaint = Paint().apply {
            color = Color.BLACK; textSize = 42f; isFakeBoldText = true
            isAntiAlias = true; textAlign = Paint.Align.CENTER
        }
        canvas.drawText(student.studentName.ifBlank { "-" }, centerX, y, namePaint)
        y += 50f

        val dividerPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 2f }
        canvas.drawLine(cardLeft + 60f, y, cardRight - 60f, y, dividerPaint)
        y += 55f

        // ---------------- Remaining details — values only, no labels ----------------
        val valuePaint = Paint().apply {
            color = Color.DKGRAY; textSize = 32f; isAntiAlias = true; textAlign = Paint.Align.CENTER
        }

        val values = listOf(
            student.fatherName,
            student.className,
            student.rollNumber,
            student.mobileNumber
        )

        values.forEach { value ->
            canvas.drawText(value.ifBlank { "-" }, centerX, y, valuePaint)
            y += 55f
        }

        // ---------------- Notes (wrapped, only if present) ----------------
        if (student.notes.isNotBlank()) {
            y += 15f
            val notesPaint = Paint().apply {
                color = Color.DKGRAY; textSize = 26f; isAntiAlias = true; textAlign = Paint.Align.CENTER
            }
            val maxWidth = cardRight - cardLeft - 120f
            wrapText(student.notes, notesPaint, maxWidth).forEach { line ->
                if (y < cardBottom - 30f) {
                    canvas.drawText(line, centerX, y, notesPaint)
                    y += 36f
                }
            }
        }

        // ---------------- Footer ----------------
        val footerPaint = Paint().apply {
            color = Color.GRAY; textSize = 24f; isAntiAlias = true; textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Page $pageNumber of $totalPages", centerX, PAGE_HEIGHT - 60f, footerPaint)
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
     * (photos need no alpha channel) — this is the key fix for the large
     * export size: previously the full ~800px optimized photo was decoded
     * and embedded even though the PDF only displays it at ~380pt.
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
