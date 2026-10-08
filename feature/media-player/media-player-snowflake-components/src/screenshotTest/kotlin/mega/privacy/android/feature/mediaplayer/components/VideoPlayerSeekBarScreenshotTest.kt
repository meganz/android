package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot tests for [VideoPlayerSeekBar].
 *
 * Drawn on black rather than on the preview surface, because the bar sits on the picture in the
 * player and its text and bar colours are fixed for that background. On a surface that follows
 * the theme the light golden would show white text on white and prove nothing.
 *
 * The two themes are expected to differ, in the played part of the bar and the scrubber only.
 * Those take `DSTokens.colors.brand.default`, which follows the theme exactly as the live player's
 * `played_color`/`scrubber_color` do — `@color/color_button_brand` is `#DD1405` by day and
 * `#F23433` by night. Everything else is fixed, so [VideoPlayerSeekBarWithoutDuration], which
 * draws no played part, is identical in both.
 */
class VideoPlayerSeekBarScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun VideoPlayerSeekBarAtStart() {
        OnPicture {
            VideoPlayerSeekBar(
                currentPositionMs = 0L,
                durationMs = DURATION_MS,
                onSeekTo = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun VideoPlayerSeekBarPartwayThrough() {
        OnPicture {
            VideoPlayerSeekBar(
                currentPositionMs = 72_000L,
                durationMs = DURATION_MS,
                onSeekTo = {},
            )
        }
    }

    /** What the bar looks like before the player has reported a length. */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun VideoPlayerSeekBarWithoutDuration() {
        OnPicture {
            VideoPlayerSeekBar(
                currentPositionMs = 0L,
                durationMs = 0L,
                onSeekTo = {},
            )
        }
    }

    @Composable
    private fun OnPicture(content: @Composable () -> Unit) {
        AndroidThemeForPreviews {
            Box(modifier = Modifier.fillMaxWidth().background(Color.Black)) {
                content()
            }
        }
    }
}

private const val DURATION_MS = 225_000L
