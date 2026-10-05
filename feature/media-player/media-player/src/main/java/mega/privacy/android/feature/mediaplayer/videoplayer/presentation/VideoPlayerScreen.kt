package mega.privacy.android.feature.mediaplayer.videoplayer.presentation

import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerUiState

/**
 * Screen of the revamped video player.
 *
 * Shows the picture and nothing else; controls, the queue, subtitles and the rest arrive one
 * feature at a time against `.ref/video-player-feature-inventory.md`, each bringing the state and
 * callbacks it needs.
 *
 * Stateless by convention — it never takes the ViewModel or a nav key.
 */
@Composable
internal fun VideoPlayerScreen(
    uiState: VideoPlayerUiState,
    onSurfaceReady: (TextureView) -> Unit,
    onSurfaceReleased: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (uiState is VideoPlayerUiState.Data) {
            VideoSurface(
                videoWidth = uiState.videoWidth,
                videoHeight = uiState.videoHeight,
                onSurfaceReady = onSurfaceReady,
                onSurfaceReleased = onSurfaceReleased,
            )
        }
    }
}
