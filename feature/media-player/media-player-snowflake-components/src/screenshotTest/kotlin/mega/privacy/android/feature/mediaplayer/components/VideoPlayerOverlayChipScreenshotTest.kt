package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot tests for [VideoPlayerOverlayChip], one per [VideoPlayerOverlayChipState] branch.
 * The seek states differ in which side the icon sits on, and the zoom states select between two
 * string resources and a formatted percentage, so each is covered separately.
 */
class VideoPlayerOverlayChipScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SeekForwardChip() {
        AndroidThemeForPreviews {
            VideoPlayerOverlayChip(
                VideoPlayerOverlayChipState.Seek(seconds = 30, isForward = true)
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SeekBackwardChip() {
        AndroidThemeForPreviews {
            VideoPlayerOverlayChip(
                VideoPlayerOverlayChipState.Seek(seconds = 15, isForward = false)
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun LongPressSpeedHeldChip() {
        AndroidThemeForPreviews {
            VideoPlayerOverlayChip(
                VideoPlayerOverlayChipState.LongPressSpeedHeld(speedText = "2×")
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ZoomFitToScreenChip() {
        AndroidThemeForPreviews {
            VideoPlayerOverlayChip(VideoPlayerOverlayChipState.Zoom.FitToScreen)
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ZoomFillScreenChip() {
        AndroidThemeForPreviews {
            VideoPlayerOverlayChip(VideoPlayerOverlayChipState.Zoom.FillScreen)
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ZoomPercentageChip() {
        AndroidThemeForPreviews {
            VideoPlayerOverlayChip(VideoPlayerOverlayChipState.Zoom.Percentage(percent = 250))
        }
    }
}
