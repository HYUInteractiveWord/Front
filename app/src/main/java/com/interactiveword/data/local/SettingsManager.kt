package com.interactiveword.data.local

import android.content.Context
import androidx.core.content.edit

object SettingsManager {
    private const val PREFS_NAME = "settings_prefs"
    private const val KEY_BASE_URL = "base_url"
    const val DEFAULT_BASE_URL = "http://13.209.74.220:8000/"

    fun saveBaseUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_BASE_URL, url)
            }
    }

    fun getBaseUrl(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
}
