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
 * Recorded in both themes although the player is always dark. The background, the throbber and
 * the control icon take their colours explicitly, so the two goldens may differ in one place
 * only: the played part of the scrub bar and its scrubber, which follow the theme because the
 * live player's `played_color` and `scrubber_color` do. A difference anywhere else is the signal
 * that something started following the app theme when it should not.
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
                onSeekTo = {},
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
                onSeekTo = {},
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
                onSeekTo = {},
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
    // A position partway through, so the scrub bar's played and unplayed halves are both in the
    // golden and a colour that stopped being applied would show up as a change.
    currentPositionMs = 72_000L,
    durationMs = 225_000L,
)
