package com.interactiveword.data.local

import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import androidx.core.content.edit

object LanguageManager {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "language"

    fun saveLanguage(context: Context, language: String) {
        val current = getSavedLanguage(context)
        if (current == language) return

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_LANGUAGE, language)
            }
    }

    fun getSavedLanguage(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "en") ?: "en"

    fun applyLocale(base: Context): Context {
        val lang = getSavedLanguage(base)
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
