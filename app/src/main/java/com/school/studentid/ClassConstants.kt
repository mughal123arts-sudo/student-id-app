package com.school.studentid

/**
 * The fixed set of class "folders" shown on the Class Folders screen.
 * Opening any of these prefills the Class field when adding a new student —
 * except "Other Classes", which is a catch-all for anything not in this list
 * and is left blank for manual entry.
 */
object ClassConstants {

    val PREDEFINED_CLASSES = listOf(
        "Playgroup (PG)",
        "Montessori",
        "Nursery",
        "KG",
        "Prep",
        "Class 1",
        "Class 2",
        "Class 3",
        "Class 4",
        "Class 5",
        "Class 6",
        "Class 7",
        "Class 8",
        "Class 9",
        "Class 10",
        "Pre-9th"
    )

    const val OTHER_CLASSES = "Other Classes"

    val ALL_FOLDERS: List<String> = PREDEFINED_CLASSES + OTHER_CLASSES
}
