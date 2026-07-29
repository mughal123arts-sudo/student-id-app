package com.school.studentid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

class StudentViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = StudentDatabase.getDatabase(application).studentDao()

    private val searchQuery = MutableStateFlow("")

    val students: StateFlow<List<Student>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) dao.getAllStudents() else dao.searchStudents(query)
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun addStudent(student: Student, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            dao.insertStudent(student)
            onDone()
        }
    }

    fun updateStudent(student: Student, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            dao.updateStudent(student)
            onDone()
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            dao.deleteStudent(student)
        }
    }

    /**
     * Exports all currently loaded students to a CSV file in the app's external
     * files directory, ready to be shared or opened in Excel.
     * Returns the created File, or null if there was nothing to export.
     */
    suspend fun exportToCsv(): File? {
        val list = students.value
        if (list.isEmpty()) return null

        val exportsDir = File(getApplication<Application>().getExternalFilesDir(null), "exports")
        if (!exportsDir.exists()) exportsDir.mkdirs()

        val file = File(exportsDir, "students_export_${System.currentTimeMillis()}.csv")

        FileWriter(file).use { writer ->
            writer.append("Student Name,Father Name,Class,Roll Number,Mobile Number\n")
            list.forEach { s ->
                writer.append(
                    listOf(
                        s.studentName, s.fatherName, s.className,
                        s.rollNumber, s.mobileNumber
                    ).joinToString(",") { field -> "\"${field.replace("\"", "\"\"")}\"" }
                )
                writer.append("\n")
            }
        }
        return file
    }

    /**
     * Exports the CSV (as above) AND collects every student's saved photo file,
     * so both the data and the pictures can be shared together in one go.
     * Returns an empty list if there is nothing to export.
     */
    suspend fun exportForSharing(): List<File> {
        val csvFile = exportToCsv() ?: return emptyList()
        val photoFiles = students.value
            .mapNotNull { it.photoUri }
            .map { File(it) }
            .filter { it.exists() }
        return listOf(csvFile) + photoFiles
    }
}
