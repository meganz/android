package mega.privacy.android.feature.mediaplayer.videoplayer.presentation

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerLaunchSourceType
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerUiState

/**
 * Baseline screenshots for [VideoPlayerScreen].
 *
 * The picture itself cannot be captured — the surface is a `TextureView` with no video behind it
 * in a preview — so these goldens are about the controls and the black they sit on, which is the
 * whole of what this screen draws itself.
 *
 * Recorded in both themes although the player is always dark: the background, the throbber and
 * the control icon take their colours explicitly rather than from the theme, so the two goldens
 * are expected to be identical, and them diverging later is exactly the signal that something
 * started following the app theme when it should not.
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
                onPlayPauseClick = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun VideoPlayerPlaying() {
        AndroidThemeForPreviews {
            VideoPlayerScreen(
                uiState = videoState(isLoading = false, playWhenReady = true),
                onSurfaceReady = {},
                onSurfaceReleased = {},
                onPlayPauseClick = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun VideoPlayerPaused() {
        AndroidThemeForPreviews {
            VideoPlayerScreen(
                uiState = videoState(isLoading = false, playWhenReady = false),
                onSurfaceReady = {},
                onSurfaceReleased = {},
                onPlayPauseClick = {},
            )
        }
    }
}

private fun videoState(
    isLoading: Boolean,
    playWhenReady: Boolean = false,
) = VideoPlayerUiState.Data(
    currentPlayingHandle = 1L,
    currentPlayingItemName = "video.mp4",
    launchSourceType = VideoPlayerLaunchSourceType.VideoBrowse,
    videoWidth = 1920,
    videoHeight = 1080,
    isLoading = isLoading,
    playWhenReady = playWhenReady,
)
