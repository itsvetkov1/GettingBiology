package com.znam.app

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    const val PREFS_NAME = "QuizPrefs"
    const val KEY_APP_LANGUAGE = "app_language"
    const val LANGUAGE_BG = "bg"
    const val LANGUAGE_EN = "en"

    fun applyLocale(context: Context): Context {
        return updateLocale(context, getSavedLanguage(context))
    }

    fun getSavedLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APP_LANGUAGE, LANGUAGE_BG)
            .takeIf { it == LANGUAGE_EN || it == LANGUAGE_BG }
            ?: LANGUAGE_BG
    }

    fun setSavedLanguage(context: Context, language: String) {
        val normalized = if (language == LANGUAGE_EN) LANGUAGE_EN else LANGUAGE_BG
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_LANGUAGE, normalized)
            .apply()
    }

    private fun updateLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        return context.createConfigurationContext(configuration)
    }
}
