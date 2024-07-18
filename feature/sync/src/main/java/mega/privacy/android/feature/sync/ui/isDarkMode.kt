package mega.privacy.android.feature.sync.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import mega.privacy.android.domain.entity.ThemeMode

/**
 * Is current theme mode a dark theme
 */
@Composable
internal fun ThemeMode.isDarkMode() = when (this) {
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
    ThemeMode.System -> isSystemInDarkTheme()
}