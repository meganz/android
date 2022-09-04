package mega.privacy.android.presentation.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable


/**
 * Android theme
 *
 * @param isDark
 * @param content
 */
@Composable
fun AndroidTheme(
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = if (isDark) {
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

