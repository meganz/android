package mega.privacy.android.core.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController


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

    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = colors.primary,
            darkIcons = !isDark
        )
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

