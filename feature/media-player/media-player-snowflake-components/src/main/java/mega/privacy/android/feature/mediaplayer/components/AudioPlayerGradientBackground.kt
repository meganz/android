package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * @param reversed When `true`, the gradient runs from [mega.android.core.ui.tokens.theme.DSTokens.colors.background.pageBackground]
 *   (top) to [mega.android.core.ui.tokens.theme.DSTokens.colors.brand.containerDefault] (bottom),
 *   inverting the default direction.
 */
@Composable
fun AudioPlayerGradientBackground(
    modifier: Modifier = Modifier,
    reversed: Boolean = false,
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
        modifier = modifier.background(
            Brush.verticalGradient(colors = colors)
        ),
        content = content,
    )
}
