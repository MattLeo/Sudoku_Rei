package com.sudokurei.data

import android.content.Context
import com.sudokurei.viewmodel.ThemeMode
import androidx.core.content.edit

class ThemeRepository(context: Context) {

    private val prefs = context.getSharedPreferences("sudoku_settings", Context.MODE_PRIVATE)

    fun saveTheme(mode: ThemeMode) {
        prefs.edit { putString("theme_mode", mode.name) }
    }

    fun loadTheme(): ThemeMode {
        val name = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
        return try { ThemeMode.valueOf(name!!) } catch (_: Exception) { ThemeMode.SYSTEM }
    }

    fun saveShowTimer(show: Boolean) {
        prefs.edit { putBoolean("show_timer", show) }
    }

    fun loadShowTimer(): Boolean = prefs.getBoolean("show_timer", true)

    fun saveAutoPause(enabled: Boolean) {
        prefs.edit { putBoolean("auto_pause", enabled) }
    }

    fun loadAutoPause(): Boolean = prefs.getBoolean("auto_pause", true)
}