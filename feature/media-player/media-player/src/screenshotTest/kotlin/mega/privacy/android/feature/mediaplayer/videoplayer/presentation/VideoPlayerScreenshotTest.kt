package mega.privacy.android.feature.mediaplayer.videoplayer.presentation

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerLaunchSourceType
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerUiState

/**
 * Baseline screenshots for the loading state of [VideoPlayerScreen].
 *
 * The picture itself cannot be captured — the surface is a `TextureView` with no video behind it
 * in a preview — so these goldens are about the spinner and the black it sits on, which is the
 * whole of what this screen draws itself.
 *
 * Recorded in both themes although the player is always dark: the background and the throbber
 * take their colours explicitly rather than from the theme, so the two goldens are expected to be
 * identical, and they diverging later is exactly the signal that something started following the
 * app theme when it should not.
 */
class VideoPlayerScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun VideoPlayerLoading() {
        AndroidThemeForPreviews {
            VideoPlayerScreen(
                uiState = videoState(isLoading = true),
                onSurfaceReady = {},
                onSurfaceReleased = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun VideoPlayerPlaying() {
        AndroidThemeForPreviews {
            VideoPlayerScreen(
                uiState = videoState(isLoading = false),
                onSurfaceReady = {},
                onSurfaceReleased = {},
            )
        }
    }
}

private fun videoState(isLoading: Boolean) = VideoPlayerUiState.Data(
    currentPlayingHandle = 1L,
    currentPlayingItemName = "video.mp4",
    launchSourceType = VideoPlayerLaunchSourceType.VideoBrowse,
    videoWidth = 1920,
    videoHeight = 1080,
    isLoading = isLoading,
)
