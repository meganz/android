package mega.privacy.android.app.presentation.videoplayer.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.videoplayer.VideoPlayerViewModelV2
import mega.privacy.android.app.presentation.videoplayer.view.VideoPlayerScreen
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberSingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler

@Serializable
internal data object VideoPlayerScreenNavKey : NavKey

internal fun EntryProviderScope<NavKey>.videoPlayerScreen(
    navigationHandler: NavigationHandler,
    viewModel: VideoPlayerViewModelV2,
    player: ExoPlayer?,
    handleAutoReplayIfPaused: () -> Unit,
    navigate: (NavKey) -> Unit,
    onMoreActionsClicked: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<VideoPlayerScreenNavKey> {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) {
            handleAutoReplayIfPaused()
        }

        LaunchedEffect(uiState.navigateToSelectSubtitleScreen) {
            if (uiState.navigateToSelectSubtitleScreen) {
                navigate(VideoPlayerSelectSubtitleScreenNavKey)
                viewModel.updateNavigateToSelectSubtitle(false)
            }
        }

        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                creationCallback = { it.create(viewModel.uiState.value.nodeSourceType) }
            )
        val nodeActionHandler = rememberSingleNodeActionHandler(
            viewModel = nodeOptionsActionViewModel,
            navigationHandler = navigationHandler,
        )
        HandleNodeOptionsActionResult(
            nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            navigationHandler = navigationHandler,
            nodeActionHandler = nodeActionHandler,
            onTransfer = onTransfer,
        )

        VideoPlayerScreen(
            viewModel = viewModel,
            player = player,
            playQueueButtonClicked = {
                navigate(VideoPlayerQueueScreenNavKey)
            },
            onMoreActionsClicked = onMoreActionsClicked,
        )
    }
}
