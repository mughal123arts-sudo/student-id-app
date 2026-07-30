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
    private val classFilter = MutableStateFlow<String?>(null)

    /** Students for the currently selected class folder — empty until a folder is chosen. */
    val students: StateFlow<List<Student>> = combine(searchQuery, classFilter) { query, cls -> query to cls }
        .flatMapLatest { (query, cls) ->
            when {
                cls == null -> flowOf(emptyList())
                cls == ClassConstants.OTHER_CLASSES ->
                    if (query.isBlank()) dao.getStudentsInOtherClasses(ClassConstants.PREDEFINED_CLASSES)
                    else dao.searchStudentsInOtherClasses(ClassConstants.PREDEFINED_CLASSES, query)
                else ->
                    if (query.isBlank()) dao.getStudentsByClass(cls)
                    else dao.searchStudentsInClass(cls, query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    /** Call when entering a class folder (or "Other Classes") to scope the student list to it. */
    fun setClassFilter(className: String) {
        if (classFilter.value != className) {
            classFilter.value = className
            searchQuery.value = ""
        }
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
     * Generates a single PDF for the students currently shown (the open
     * class folder) — one page per student, photo on top, details below —
     * ready to be shared. Returns null if there's nothing to export or
     * generation failed for some reason (never throws/crashes the caller).
     */
    suspend fun exportPdf(): File? = withContext(Dispatchers.IO) {
        try {
            PdfExporter.generateStudentsPdf(getApplication(), students.value)
        } catch (e: Exception) {
            null
        }
    }
}
