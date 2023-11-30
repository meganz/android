package mega.privacy.android.core.ui.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
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
import mega.privacy.android.core.ui.theme.tokens.Background
import mega.privacy.android.core.ui.theme.tokens.Border
import mega.privacy.android.core.ui.theme.tokens.Button
import mega.privacy.android.core.ui.theme.tokens.Components
import mega.privacy.android.core.ui.theme.tokens.DefaultSemanticTokensDark
import mega.privacy.android.core.ui.theme.tokens.DefaultSemanticTokensLight
import mega.privacy.android.core.ui.theme.tokens.Focus
import mega.privacy.android.core.ui.theme.tokens.Icon
import mega.privacy.android.core.ui.theme.tokens.Indicator
import mega.privacy.android.core.ui.theme.tokens.Link
import mega.privacy.android.core.ui.theme.tokens.Notifications
import mega.privacy.android.core.ui.theme.tokens.SemanticTokens
import mega.privacy.android.core.ui.theme.tokens.Support
import mega.privacy.android.core.ui.theme.tokens.Text
import mega.privacy.android.core.ui.theme.tokens.TextColor


/**
 * Android theme to be used only for internal previews
 *
 * @param isDark
 * @param content
 */
@SuppressLint("IsSystemInDarkTheme")
@Composable
internal fun AndroidTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) = AndroidTheme(
    isDark = isDark,
    darkColorTokens = DefaultSemanticTokensDark,
    lightColorTokens = DefaultSemanticTokensLight,
    content = content,
)

/**
 * Android theme
 *
 * @param isDark
 * @param darkColorTokens [SemanticTokens] for dark mode
 * @param lightColorTokens [SemanticTokens] for light mode
 * @param content
 */
@Composable
fun AndroidTheme(
    isDark: Boolean,
    darkColorTokens: SemanticTokens,
    lightColorTokens: SemanticTokens,
    content: @Composable () -> Unit,
) {
    val legacyColors = if (isDark) {
        LegacyDarkColorPalette
    } else {
        LegacyLightColorPalette
    }

    val semanticTokens = if (isDark) {
        darkColorTokens
    } else {
        lightColorTokens
    }
    val colors = MegaColors(semanticTokens, !isDark)

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

/**
 * [MegaColors] default palette for testing purposes, all magenta to easily detect it.
 */
private val testColorPalette = MegaColors(
    object : SemanticTokens {
        override val focus = Focus()
        override val indicator = Indicator()
        override val support = Support()
        override val button = Button()
        override val text = Text()
        override val background = Background()
        override val icon = Icon()
        override val components = Components()
        override val link = Link()
        override val notifications = Notifications()
        override val border = Border()
    },
    false,
)


