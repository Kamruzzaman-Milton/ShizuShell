package com.example.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    DARK,
    AMOLED,
    LIGHT,
    SYSTEM
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val fontSizeSp: Float = 13f,
    val wrapLongLines: Boolean = true,
    val guardDangerousCommands: Boolean = true,
    val exportFolderUri: String? = null
)

class AppPreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("shizushell_prefs", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private fun loadPreferences(): UserPreferences {
        val themeStr = prefs.getString("theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val theme = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.DARK)
        val fontSize = prefs.getFloat("font_size", 13f)
        val wrap = prefs.getBoolean("wrap_long_lines", true)
        val guard = prefs.getBoolean("guard_dangerous_commands", true)
        val exportUri = prefs.getString("export_folder_uri", null)

        return UserPreferences(
            themeMode = theme,
            fontSizeSp = fontSize,
            wrapLongLines = wrap,
            guardDangerousCommands = guard,
            exportFolderUri = exportUri
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _preferences.value = _preferences.value.copy(themeMode = mode)
    }

    fun setFontSize(size: Float) {
        prefs.edit().putFloat("font_size", size).apply()
        _preferences.value = _preferences.value.copy(fontSizeSp = size)
    }

    fun setWrapLongLines(wrap: Boolean) {
        prefs.edit().putBoolean("wrap_long_lines", wrap).apply()
        _preferences.value = _preferences.value.copy(wrapLongLines = wrap)
    }

    fun setGuardDangerousCommands(guard: Boolean) {
        prefs.edit().putBoolean("guard_dangerous_commands", guard).apply()
        _preferences.value = _preferences.value.copy(guardDangerousCommands = guard)
    }

    fun setExportFolderUri(uri: String?) {
        prefs.edit().putString("export_folder_uri", uri).apply()
        _preferences.value = _preferences.value.copy(exportFolderUri = uri)
    }
}
