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
    val className: String,        // e.g. "8-B"
    val rollNumber: String,       // Roll Number / ID Number
    val mobileNumber: String,
    val photoUri: String?         // content:// or file:// URI of the student's photo, nullable
)
