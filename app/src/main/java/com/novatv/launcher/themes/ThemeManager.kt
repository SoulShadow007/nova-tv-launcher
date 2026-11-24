package com.novatv.launcher.themes

import android.content.SharedPreferences
import com.novatv.launcher.R
import com.novatv.launcher.ui.Prefs

enum class LauncherTheme(val key: String, val labelRes: Int, val themeRes: Int) {
    NOVA_DARK("nova_dark", R.string.theme_nova_dark, R.style.Theme_NovaTV_Dark),
    AURORA("aurora", R.string.theme_aurora, R.style.Theme_NovaTV_Aurora),
    AMBER_RETRO("amber", R.string.theme_amber, R.style.Theme_NovaTV_Amber),
    CRYSTAL("crystal", R.string.theme_crystal, R.style.Theme_NovaTV_Crystal);

    companion object {
        fun byKey(key: String?): LauncherTheme =
            values().firstOrNull { it.key == key } ?: NOVA_DARK
    }
}

class ThemeManager(private val prefs: SharedPreferences) {

    var current: LauncherTheme =
        LauncherTheme.byKey(prefs.getString(Prefs.KEY_THEME, null))
        private set

    fun apply(theme: LauncherTheme) {
        prefs.edit().putString(Prefs.KEY_THEME, theme.key).apply()
        current = theme
    }
}
