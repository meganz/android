package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * @param reversed When `true`, the gradient runs from [mega.android.core.ui.tokens.theme.DSTokens.colors.background.pageBackground]
 *   (top) to [mega.android.core.ui.tokens.theme.DSTokens.colors.brand.containerDefault] (bottom),
 *   inverting the default direction.
 * @param opaque When `true`, an opaque page background is painted beneath the gradient. The
 *   gradient colours are partly translucent, so anything drawn underneath shows through unless
 *   this is set — required when the background covers other content rather than the window.
 */
@Composable
fun AudioPlayerGradientBackground(
    modifier: Modifier = Modifier,
    reversed: Boolean = false,
    opaque: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = if (reversed) {
        listOf(
            DSTokens.colors.background.pageBackground,
            DSTokens.colors.brand.containerDefault,
        )
    } else {
        listOf(
            DSTokens.colors.brand.containerDefault,
            DSTokens.colors.background.pageBackground,
        )
    }
    Box(
        modifier = modifier
            .then(
                if (opaque) {
                    Modifier.background(DSTokens.colors.background.pageBackground)
                } else {
                    Modifier
                }
            )
            .background(Brush.verticalGradient(colors = colors)),
        content = content,
    )
}

@Preview(name = "Opaque over content", widthDp = 200, heightDp = 200)
@Composable
private fun PreviewAudioPlayerGradientBackgroundOpaque() {
    AndroidTheme(isDark = true) {
        Box(modifier = Modifier.size(200.dp).background(Color.Red)) {
            AudioPlayerGradientBackground(
                modifier = Modifier.fillMaxSize(),
                reversed = true,
                opaque = true,
            )
        }
    }
}

@Preview(name = "Translucent over content", widthDp = 200, heightDp = 200)
@Composable
private fun PreviewAudioPlayerGradientBackgroundTranslucent() {
    AndroidTheme(isDark = true) {
        Box(modifier = Modifier.size(200.dp).background(Color.Red)) {
            AudioPlayerGradientBackground(
                modifier = Modifier.fillMaxSize(),
                reversed = true,
            )
        }
    }
}
