package com.school.studentid

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    @Query("SELECT * FROM students ORDER BY className, rollNumber")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :studentId")
    suspend fun getStudentById(studentId: Int): Student?

    @Query("""
        SELECT * FROM students
        WHERE studentName LIKE '%' || :query || '%'
           OR fatherName LIKE '%' || :query || '%'
           OR rollNumber LIKE '%' || :query || '%'
           OR className LIKE '%' || :query || '%'
        ORDER BY className, rollNumber
    """)
    fun searchStudents(query: String): Flow<List<Student>>

    // ---- Class-folder scoped queries ----

    @Query("SELECT * FROM students WHERE className = :className ORDER BY rollNumber")
    fun getStudentsByClass(className: String): Flow<List<Student>>

    @Query("""
        SELECT * FROM students
        WHERE className = :className
        AND (studentName LIKE '%' || :query || '%'
             OR fatherName LIKE '%' || :query || '%'
             OR rollNumber LIKE '%' || :query || '%')
        ORDER BY rollNumber
    """)
    fun searchStudentsInClass(className: String, query: String): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE className NOT IN (:predefinedClasses) ORDER BY className, rollNumber")
    fun getStudentsInOtherClasses(predefinedClasses: List<String>): Flow<List<Student>>

    @Query("""
        SELECT * FROM students
        WHERE className NOT IN (:predefinedClasses)
        AND (studentName LIKE '%' || :query || '%'
             OR fatherName LIKE '%' || :query || '%'
             OR rollNumber LIKE '%' || :query || '%')
        ORDER BY className, rollNumber
    """)
    fun searchStudentsInOtherClasses(predefinedClasses: List<String>, query: String): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    @Query("SELECT COUNT(*) FROM students")
    suspend fun getStudentCount(): Int
}
