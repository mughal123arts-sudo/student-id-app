package com.school.studentid

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    private const val PAGE_WIDTH = 1240
    private const val PAGE_HEIGHT = 1754

    fun generateStudentsPdf(context: Context, students: List<Student>): File? {
        if (students.isEmpty()) return null

        val document = PdfDocument()
        return try {
            students.forEachIndexed { index, student ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                val page = document.startPage(pageInfo)
                drawStudentPage(page.canvas, student)
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

    private fun drawStudentPage(canvas: Canvas, student: Student) {
        val centerX = PAGE_WIDTH / 2f

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 44f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val labelPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 32f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 32f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 2f
        }
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.GRAY
            strokeWidth = 3f
        }
        val noPhotoPaint = Paint().apply {
            color = Color.GRAY
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        var y = 90f
        canvas.drawText("Student ID Record", centerX, y, titlePaint)
        y += 70f

        val photoSize = 420f
        val photoLeft = centerX - photoSize / 2
        val photoTop = y

        var photoDrawn = false
        student.photoUri?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    val destRect = RectF(photoLeft, photoTop, photoLeft + photoSize, photoTop + photoSize)
                    val photoPaint = Paint().apply {
                        isAntiAlias = true
                        isFilterBitmap = true
                    }
                    canvas.drawBitmap(bitmap, null, destRect, photoPaint)
                    photoDrawn = true
                }
            }
        }

        if (!photoDrawn) {
            canvas.drawRect(photoLeft, photoTop, photoLeft + photoSize, photoTop + photoSize, borderPaint)
            canvas.drawText("No Photo", centerX, photoTop + photoSize / 2, noPhotoPaint)
        } else {
            canvas.drawRect(photoLeft, photoTop, photoLeft + photoSize, photoTop + photoSize, borderPaint)
        }

        y = photoTop + photoSize + 70f
        canvas.drawLine(90f, y, PAGE_WIDTH - 90f, y, linePaint)
        y += 60f

        val fields = listOf(
            "Student Name" to student.studentName,
            "Father Name" to student.fatherName,
            "Class" to student.className,
            "Roll Number / ID Number" to student.rollNumber,
            "Contact / Mobile Number" to student.mobileNumber
        )

        val labelX = 130f
        val valueX = 520f

        fields.forEach { (label, value) ->
            canvas.drawText("$label:", labelX, y, labelPaint)
            canvas.drawText(value.ifBlank { "-" }, valueX, y, valuePaint)
            y += 66f
        }
    }
}
