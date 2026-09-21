package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.tokens.theme.DSTokens

private val BarHeight = 2.dp

/**
 * Non-interactive playback progress line drawn along the bottom edge of the mini player.
 *
 * Lives in the snowflake module because it paints raw design tokens. Deliberately not a slider:
 * the mini player only reports progress, seeking belongs to the full-screen player.
 *
 * @param progress Progress in `0f..1f`; values outside the range are clamped and `NaN` reads as
 * empty. `coerceIn` alone returns `NaN` unchanged, and `fillMaxWidth` rejects it.
 * @param modifier Modifier.
 */
@Composable
fun AudioMiniPlayerProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val playedFraction = if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight)
            .background(DSTokens.colors.background.surface2),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(playedFraction)
                .fillMaxHeight()
                .background(DSTokens.colors.brand.default),
        )
    }
}

@Preview
@Composable
private fun AudioMiniPlayerProgressBarPreview() {
    AndroidThemeForPreviews {
        AudioMiniPlayerProgressBar(progress = 0.35f)
    }
}
