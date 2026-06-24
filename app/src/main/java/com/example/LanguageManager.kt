package com.example

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object LanguageManager {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_LANGUAGE = "app_language"
    
    private val _currentLanguage = MutableStateFlow<String>("en")
    val currentLanguage: StateFlow<String> = _currentLanguage
    
    fun init(context: Context) {
        _currentLanguage.value = getLanguage(context)
    }
    
    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, null) ?: run {
            val sysLang = android.content.res.Resources.getSystem().configuration.locales.get(0).language
            if (sysLang == "ru") "ru" else "en"
        }
    }
    
    fun setLanguage(context: Context, lang: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        _currentLanguage.value = lang
        if (context is android.app.Activity) {
            context.recreate()
        }
    }
}
