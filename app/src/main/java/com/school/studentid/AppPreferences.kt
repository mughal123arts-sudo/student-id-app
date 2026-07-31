package com.school.studentid

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores small, permanent app settings on-device: the school's name/address
 * (collected once on first launch) and the last-selected class-folder view
 * mode (grid or list).
 */
object AppPreferences {
    private const val PREFS_NAME = "school_id_app_prefs"
    private const val KEY_SETUP_COMPLETE = "setup_complete"
    private const val KEY_SCHOOL_NAME = "school_name"
    private const val KEY_SCHOOL_ADDRESS = "school_address"
    private const val KEY_VIEW_MODE = "folder_view_mode" // "grid" or "list"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isSetupComplete(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SETUP_COMPLETE, false)

    fun saveSchoolInfo(context: Context, name: String, address: String) {
        prefs(context).edit()
            .putString(KEY_SCHOOL_NAME, name)
            .putString(KEY_SCHOOL_ADDRESS, address)
            .putBoolean(KEY_SETUP_COMPLETE, true)
            .apply()
    }

    fun getSchoolName(context: Context): String =
        prefs(context).getString(KEY_SCHOOL_NAME, "") ?: ""

    fun getSchoolAddress(context: Context): String =
        prefs(context).getString(KEY_SCHOOL_ADDRESS, "") ?: ""

    fun getViewMode(context: Context): String =
        prefs(context).getString(KEY_VIEW_MODE, "grid") ?: "grid"

    fun setViewMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_VIEW_MODE, mode).apply()
    }
}
