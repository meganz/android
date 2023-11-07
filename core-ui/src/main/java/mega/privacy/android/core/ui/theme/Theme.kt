package mega.privacy.android.core.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import mega.privacy.android.core.ui.theme.tokens.TextColor


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
    val legacyColors = if (isDark) {
        LegacyDarkColorPalette
    } else {
        LegacyLightColorPalette
    }

    val colors = if (isDark) {
        darkColorPalette
    } else {
        lightColorPalette
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val systemUiController = rememberSystemUiController()
        DisposableEffect(systemUiController, isDark) {
            systemUiController.setSystemBarsColor(
                color = legacyColors.primary,
                darkIcons = !isDark
            )
            onDispose { }
        }
    }
    val colorPalette by remember(colors) {
        mutableStateOf(colors)
    }
    CompositionLocalProvider(
        LocalMegaColors provides colorPalette,
    ) {
        //we need to keep `MaterialTheme` for now as not all the components are migrated to our Design System.
        MaterialTheme(
            colors = legacyColors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

internal object MegaTheme {
    val colors: MegaColors
        @Composable
        get() = LocalMegaColors.current

    @Composable
    fun textColor(textColor: TextColor) =
        LocalMegaColors.current.text.getTextColor(textColor)
}

@Suppress("PrivatePropertyName") //convention for staticCompositionLocalOf is Uppercase
private val LocalMegaColors = staticCompositionLocalOf {
    testColorPalette
}


