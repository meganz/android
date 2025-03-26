package mega.privacy.android.shared.original.core.ui.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import mega.android.core.ui.theme.values.BackgroundColor
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.LinkColor
import mega.android.core.ui.theme.values.SupportColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.tokens.AndroidNewSemanticTokensDark
import mega.android.core.ui.tokens.theme.tokens.AndroidNewSemanticTokensLight
import mega.android.core.ui.tokens.theme.tokens.Background
import mega.android.core.ui.tokens.theme.tokens.Border
import mega.android.core.ui.tokens.theme.tokens.Brand
import mega.android.core.ui.tokens.theme.tokens.Button
import mega.android.core.ui.tokens.theme.tokens.Components
import mega.android.core.ui.tokens.theme.tokens.Focus
import mega.android.core.ui.tokens.theme.tokens.Icon
import mega.android.core.ui.tokens.theme.tokens.Indicator
import mega.android.core.ui.tokens.theme.tokens.Link
import mega.android.core.ui.tokens.theme.tokens.Notifications
import mega.android.core.ui.tokens.theme.tokens.SemanticTokens
import mega.android.core.ui.tokens.theme.tokens.Support
import mega.android.core.ui.tokens.theme.tokens.Text

/**
 * Android original theme to be used only in Previews.
 *
 * @param content
 */
@SuppressLint("IsSystemInDarkTheme")
@Composable
internal fun OriginalThemeForPreviews(
    content: @Composable () -> Unit,
) = OriginalTheme(
    isSystemInDarkTheme(),
    content,
)

/**
 * Android theme to be used by components in the original design system
 *
 * @param isDark
 * @param content
 */
@Composable
fun OriginalTheme(
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val legacyColors = if (isDark) {
        LegacyDarkColorPalette
    } else {
        LegacyLightColorPalette
    }

    val semanticTokens = if (isDark) {
        AndroidNewSemanticTokensDark
    } else {
        AndroidNewSemanticTokensLight
    }
    val colors = MegaColors(semanticTokens, !isDark)

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

internal object MegaOriginalTheme {
    val colors: MegaColors
        @Composable
        get() = LocalMegaColors.current

    @Composable
    fun textColor(textColor: TextColor) = textColor.getTextColor(LocalMegaColors.current.text)

    @Composable
    fun backgroundColor(backgroundColor: BackgroundColor) =
        backgroundColor.getBackgroundColor(LocalMegaColors.current.background)

    @Composable
    fun iconColor(iconColor: IconColor) = iconColor.getIconColor(LocalMegaColors.current.icon)

    @Composable
    fun supportColor(supportColor: SupportColor) =
        supportColor.getSupportColor(LocalMegaColors.current.support)

    @Composable
    fun linkColor(linkColor: LinkColor) =
        linkColor.getLinkColor(LocalMegaColors.current.link)

}

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
        override val brand = Brand()
    },
    false,
)


