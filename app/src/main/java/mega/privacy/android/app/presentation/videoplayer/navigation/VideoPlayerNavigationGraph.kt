package mega.privacy.android.app.presentation.videoplayer.navigation

import androidx.compose.material.ScaffoldState
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModelV2
import mega.privacy.android.navigation.contract.NavigationHandler

internal fun EntryProviderScope<NavKey>.videoPlayerEntryProvider(
    navigationHandler: NavigationHandler,
    scaffoldState: ScaffoldState,
    viewModel: VideoPlayerViewModelV2,
    player: ExoPlayer?,
    handleAutoReplayIfPaused: () -> Unit,
) {
    videoPlayerScreen(
        scaffoldState = scaffoldState,
        viewModel = viewModel,
        player = player,
        handleAutoReplayIfPaused = handleAutoReplayIfPaused,
        playQueueButtonClicked = {
            navigationHandler.navigate(VideoPlayerQueueScreenNavKey)
        },
        onNavigateToSelectSubtitle = {
            navigationHandler.navigate(VideoPlayerSelectSubtitleScreenNavKey)
        },
    )

    videoPlayerQueueScreen(
        viewModel = viewModel,
        onBack = navigationHandler::back,
    )

    videoPlayerSelectSubtitleScreen(
        viewModel = viewModel,
        onBack = navigationHandler::back,
    )
}
