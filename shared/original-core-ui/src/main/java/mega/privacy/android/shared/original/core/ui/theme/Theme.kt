package mega.privacy.android.shared.original.core.ui.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.tokens.theme.tokens.AndroidNewSemanticTokensDark
import mega.android.core.ui.tokens.theme.tokens.AndroidNewSemanticTokensLight
import mega.android.core.ui.tokens.theme.tokens.Background
import mega.android.core.ui.tokens.theme.tokens.Border
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
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.values.BackgroundColor
import mega.privacy.android.shared.original.core.ui.theme.values.TempSemanticTokensDark
import mega.privacy.android.shared.original.core.ui.theme.values.TempSemanticTokensLight
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Android theme to be used by Original components with TEMP (temporary) color tokens.
 *
 * @param isDark
 * @param content
 */
@Composable
fun OriginalTempTheme(
    isDark: Boolean,
    content: @Composable () -> Unit,
) = OriginalTheme(
    isDark = isDark,
    darkColorTokens = TempSemanticTokensDark,
    lightColorTokens = TempSemanticTokensLight,
    content = content,
)

/**
 * Android theme with TEMP (temporary) tokens to be used only in Previews.
 *
 * @param content
 */
@SuppressLint("IsSystemInDarkTheme")
@Composable
internal fun OriginalTempThemeForPreviews(
    content: @Composable () -> Unit,
) = OriginalTempTheme(
    isSystemInDarkTheme(),
    content,
)

/**
 * Helper function to create preview with both, Android TEMP and Android NEW tokens.
 * This should only be used for previews with the objective of compare the differences between the 2 core-tokens and how they work with the components.
 */
@SuppressLint("IsSystemInDarkTheme")
@Composable
internal fun PreviewWithTempAndNewCoreColorTokens(
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) = Column {
    OriginalTheme(
        isDark = isDark,
        darkColorTokens = TempSemanticTokensDark,
        lightColorTokens = TempSemanticTokensLight,
        content = {
            PreviewWithTitle(title = "TEMP", content)
        }
    )
    OriginalTheme(
        isDark = isDark,
        darkColorTokens = AndroidNewSemanticTokensDark,
        lightColorTokens = AndroidNewSemanticTokensLight,
        content = {
            PreviewWithTitle(title = "NEW", content)
        }
    )
}

@Composable
private fun PreviewWithTitle(title: String, content: @Composable () -> Unit) =
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        MegaText(
            text = title,
            textColor = TextColor.Accent,
            style = MaterialTheme.typography.body2,
        )
        content()
    }

/**
 * Android theme to be used by Original components with with the specified color tokens.
 * This method is added to add flexibility, however the version with default tokens is preferred: [OriginalTempTheme].
 *
 * @param isDark
 * @param darkColorTokens [SemanticTokens] for dark mode
 * @param lightColorTokens [SemanticTokens] for light mode
 * @param content
 */
@Composable
fun OriginalTheme(
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
    },
    false,
)


