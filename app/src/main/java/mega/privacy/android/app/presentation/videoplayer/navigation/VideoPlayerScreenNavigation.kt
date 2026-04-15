package mega.privacy.android.app.presentation.videoplayer.navigation

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModelV2
import mega.privacy.android.app.presentation.videoplayer.view.VideoPlayerScreen

@Serializable
internal data object VideoPlayerScreenNavKey : NavKey

internal fun EntryProviderScope<NavKey>.videoPlayerScreen(
    scaffoldState: ScaffoldState,
    viewModel: VideoPlayerViewModelV2,
    player: ExoPlayer?,
    handleAutoReplayIfPaused: () -> Unit,
    playQueueButtonClicked: () -> Unit,
    onNavigateToSelectSubtitle: () -> Unit,
) {
    entry<VideoPlayerScreenNavKey> {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) {
            handleAutoReplayIfPaused()
        }

        LaunchedEffect(uiState.navigateToSelectSubtitleScreen) {
            if (uiState.navigateToSelectSubtitleScreen) {
                onNavigateToSelectSubtitle()
                viewModel.updateNavigateToSelectSubtitle(false)
            }
        }

        VideoPlayerScreen(
            scaffoldState = scaffoldState,
            viewModel = viewModel,
            player = player,
            playQueueButtonClicked = playQueueButtonClicked
        )
    }
}
