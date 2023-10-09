package mega.privacy.android.core.ui.theme

import mega.privacy.android.core.ui.theme.tokens.Background
import mega.privacy.android.core.ui.theme.tokens.Border
import mega.privacy.android.core.ui.theme.tokens.Button
import mega.privacy.android.core.ui.theme.tokens.Components
import mega.privacy.android.core.ui.theme.tokens.Focus
import mega.privacy.android.core.ui.theme.tokens.Icon
import mega.privacy.android.core.ui.theme.tokens.Indicator
import mega.privacy.android.core.ui.theme.tokens.Link
import mega.privacy.android.core.ui.theme.tokens.Notifications
import mega.privacy.android.core.ui.theme.tokens.SemanticTokens
import mega.privacy.android.core.ui.theme.tokens.SemanticTokensDark
import mega.privacy.android.core.ui.theme.tokens.SemanticTokensLight
import mega.privacy.android.core.ui.theme.tokens.Support
import mega.privacy.android.core.ui.theme.tokens.Text

/**
 * [MegaColors] palette to be used in Light Theme
 */
internal val lightColorPalette = MegaColors(
    SemanticTokensLight,
    isLight = true
)

/**
 * [MegaColors] palette to be used in Dark Theme
 */
internal val darkColorPalette = MegaColors(
    SemanticTokensDark,
    isLight = false
)

/**
 * [MegaColors] default palette for testing purposes, all magenta to easily detect it.
 */
internal val testColorPalette = MegaColors(
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