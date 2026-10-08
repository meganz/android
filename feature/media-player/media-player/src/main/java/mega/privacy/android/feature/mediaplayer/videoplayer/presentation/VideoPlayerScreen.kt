package mega.privacy.android.feature.mediaplayer.videoplayer.presentation

import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.feature.mediaplayer.components.MediaPlayerLoadingIndicator
import mega.privacy.android.feature.mediaplayer.components.VideoPlayerSeekBar
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerUiState

/**
 * Screen of the revamped video player.
 *
 * Shows the picture, the play/pause button and the scrub bar with the playback times; the queue,
 * subtitles, gestures and the rest arrive one feature at a time against
 * `.ref/video-player-feature-inventory.md`, each bringing the state and callbacks it needs.
 *
 * The controls are always on screen. Showing and hiding them on a tap, and the scrim that goes
 * with it, belong to the controller-visibility work and are not here yet.
 *
 * Stateless by convention — it never takes the ViewModel or a nav key.
 */
@Composable
internal fun VideoPlayerScreen(
    uiState: VideoPlayerUiState,
    onSurfaceReady: (TextureView) -> Unit,
    onSurfaceReleased: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (uiState is VideoPlayerUiState.Data) {
            VideoSurface(
                videoWidth = uiState.videoWidth,
                videoHeight = uiState.videoHeight,
                onSurfaceReady = onSurfaceReady,
                onSurfaceReleased = onSurfaceReleased,
            )
            // Sits where the live player's spinner sits, and swaps to one while the player works
            // towards a picture, so the two never appear at once.
            VideoPlayerPlayPauseButton(
                isPlaying = uiState.playWhenReady,
                isLoading = uiState.isLoading,
                onClick = onPlayPauseClick,
                modifier = Modifier.testTag(VIDEO_PLAYER_PLAY_PAUSE_TEST_TAG),
            )
            VideoPlayerSeekBar(
                currentPositionMs = uiState.currentPositionMs,
                durationMs = uiState.durationMs,
                onSeekTo = onSeekTo,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // The host activity draws edge to edge, so without this the bar sits under
                    // the navigation bar. Goes to zero once the player hides the system bars,
                    // which is the controller-visibility work.
                    .navigationBarsPadding()
                    .padding(bottom = CONTROLS_BOTTOM_MARGIN),
            )
        } else if (uiState.showsLoadingIndicator) {
            // Only before the launch resolves: from then on the play/pause button carries the
            // spinner, as the live player's does.
            MediaPlayerLoadingIndicator(
                modifier = Modifier
                    .size(LOADING_INDICATOR_SIZE)
                    .testTag(VIDEO_PLAYER_LOADING_TEST_TAG)
            )
        }
    }
}

private val LOADING_INDICATOR_SIZE = 48.dp

/**
 * What the live player keeps between its control strip and the bottom of the screen
 * (`controls_view`, `layout_marginBottom` in `video_player_control_revamp.xml`). The row the live
 * player puts below the bar — playback speed, previous and next — is not built yet, so for now the
 * bar is the bottom of the strip.
 */
private val CONTROLS_BOTTOM_MARGIN = 30.dp

/** The spinner shown while the launch is still being resolved. */
const val VIDEO_PLAYER_LOADING_TEST_TAG = "video_player:loading"

/** The play/pause toggle, which also carries the spinner once playback has started. */
const val VIDEO_PLAYER_PLAY_PAUSE_TEST_TAG = "video_player:play_pause"
