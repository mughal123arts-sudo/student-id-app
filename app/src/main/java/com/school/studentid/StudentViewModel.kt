package com.school.studentid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class DashboardStats(
    val totalStudents: Int = 0,
    val totalClasses: Int = 0,
    val totalPhotos: Int = 0,
    val storageUsedBytes: Long = 0L
)

class StudentViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = StudentDatabase.getDatabase(application).studentDao()

    private val searchQuery = MutableStateFlow("")
    private val classFilter = MutableStateFlow<String?>(null)

    /** Every student in the database — used for the dashboard, folder counts, and lookups. */
    val allStudents: StateFlow<List<Student>> = dao.getAllStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    /** Dashboard summary: total students, distinct classes in use, total photos, and storage used. */
    val dashboardStats: StateFlow<DashboardStats> = allStudents.map { list ->
        val totalPhotos = list.count { !it.photoUri.isNullOrBlank() }
        val storageBytes = list.mapNotNull { it.photoUri }.distinct().sumOf { path ->
            val f = File(path)
            if (f.exists()) f.length() else 0L
        }
        val totalClasses = list.map { it.className }.filter { it.isNotBlank() }.distinct().size
        DashboardStats(list.size, totalClasses, totalPhotos, storageBytes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    /** Student count per class-folder (predefined classes + "Other Classes" bucket). */
    val folderCounts: StateFlow<Map<String, Int>> = allStudents.map { list ->
        val counts = mutableMapOf<String, Int>()
        ClassConstants.PREDEFINED_CLASSES.forEach { counts[it] = 0 }
        counts[ClassConstants.OTHER_CLASSES] = 0
        list.forEach { s ->
            val key = if (ClassConstants.PREDEFINED_CLASSES.contains(s.className)) s.className else ClassConstants.OTHER_CLASSES
            counts[key] = (counts[key] ?: 0) + 1
        }
        counts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ---- Global search (across every class) ----
    private val globalSearchQuery = MutableStateFlow("")
    val globalSearchResults: StateFlow<List<Student>> = globalSearchQuery
        .flatMapLatest { q -> if (q.isBlank()) flowOf(emptyList()) else dao.searchStudents(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setGlobalSearchQuery(query: String) {
        globalSearchQuery.value = query
    }

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
     * Looks up a student directly from the database (not the possibly-stale
     * in-memory list) — used right after Add/Edit navigation so the newly
     * saved record is always found immediately, even before the reactive
     * list has re-emitted.
     */
    suspend fun getStudentById(id: Int): Student? = withContext(Dispatchers.IO) {
        dao.getStudentById(id)
    }

    /**
     * Generates a single PDF for the students currently shown (the open
     * class folder) — one page per student, photo on top, details below —
     * ready to be shared. Returns null if there's nothing to export or
     * generation failed for some reason (never throws/crashes the caller).
     */
    suspend fun exportPdf(classFolder: String): File? = withContext(Dispatchers.IO) {
        try {
            PdfExporter.generateStudentsPdf(getApplication(), students.value, classFolder)
        } catch (e: Exception) {
            null
        }
    }

    /** Generates a single-page PDF ID card for one student (Share / Print). */
    suspend fun exportSingleStudentPdf(student: Student): File? = withContext(Dispatchers.IO) {
        try {
            PdfExporter.generateStudentsPdf(getApplication(), listOf(student))
        } catch (e: Exception) {
            null
        }
    }
}
