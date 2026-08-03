package com.school.studentid

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents one student's record collected for ID card generation.
 */
@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val studentName: String,
    val fatherName: String,
    val className: String,        // e.g. "Class 8" or "Playgroup (PG)"
    val section: String = "",     // e.g. "A" — optional
    val rollNumber: String,       // Roll Number / ID Number (also used as Admission No)
    val mobileNumber: String,
    val photoUri: String?,        // absolute path to the optimized photo file, nullable
    val notes: String = ""        // free-form notes about the student
)
