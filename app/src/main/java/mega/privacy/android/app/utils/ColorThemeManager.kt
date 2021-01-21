package mega.privacy.android.app.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ColorThemeManager {

    private const val COLOR_THEME_SHARED_PREFERENCE = "color_theme"

    private const val COLOR_THEME_MODE = "color_mode"

    private const val DEFAULT_THEME = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

    fun getColorTheme(context: Context) =
        context.getSharedPreferences(COLOR_THEME_SHARED_PREFERENCE, Context.MODE_PRIVATE)
            .getInt(COLOR_THEME_MODE, DEFAULT_THEME)

    fun setAndApplyColorTheme(context: Context, theme: Int) {
        context.getSharedPreferences(COLOR_THEME_SHARED_PREFERENCE, Context.MODE_PRIVATE).edit()
            .putInt(COLOR_THEME_MODE, theme).apply()
        AppCompatDelegate.setDefaultNightMode(theme)
    }


    fun applyColorTheme(context: Context) =
        AppCompatDelegate.setDefaultNightMode(getColorTheme(context))
}