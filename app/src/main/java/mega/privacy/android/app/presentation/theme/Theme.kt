package mega.privacy.android.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import mega.privacy.android.domain.entity.ThemeMode


@Composable
fun AndroidTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val darkTheme: Boolean = isAppDarkTheme(mode)
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
fun isAppDarkTheme(mode: ThemeMode) = when (mode) {
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
    ThemeMode.System -> isSystemInDarkTheme()
}

