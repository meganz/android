package mega.privacy.android.feature.mediaplayer.videoplayer.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.feature.mediaplayer.videoplayer.presentation.model.VideoPlayerUiState

/**
 * Screen of the revamped video player.
 *
 * Renders nothing yet: content arrives one feature at a time, each against its entry in
 * `.ref/video-player-feature-inventory.md`. Callbacks arrive the same way, with the feature that
 * raises them, which is why the signature has none.
 *
 * Stateless by convention — it never takes the ViewModel or a nav key.
 */
@Composable
internal fun VideoPlayerScreen(
    uiState: VideoPlayerUiState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize())
}
