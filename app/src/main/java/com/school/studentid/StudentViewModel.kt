package com.school.studentid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
     * Generates a single PDF — one page per student, photo on top and details
     * below — ready to be shared. Returns null if there are no students yet
     * or generation failed for some reason (never throws/crashes the caller).
     */
    suspend fun exportPdf(): File? = withContext(Dispatchers.IO) {
        try {
            PdfExporter.generateStudentsPdf(getApplication(), students.value)
        } catch (e: Exception) {
            null
        }
    }
}
