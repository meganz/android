package mega.privacy.android.app.mediaplayer.navigation

import android.os.Parcelable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.app.mediaplayer.AudioPlayerLaunchSourceHolder
import mega.privacy.android.app.mediaplayer.AudioPlayerScreen
import mega.privacy.android.app.mediaplayer.AudioPlayerViewModel
import mega.privacy.android.app.mediaplayer.model.AudioPlayerUiState
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberSingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.core.nodecomponents.sheet.options.DarkNodeOptionsBottomSheetNavKey
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Navigation key for the revamped audio player Compose screen.
 *
 * Only carries a [launchId] to look up the full launch payload from
 * [AudioPlayerLaunchSourceHolder], avoiding [android.os.TransactionTooLargeException].
 */
@Serializable
@Parcelize
data class AudioPlayerScreenNavKey(val launchId: String) : NavKey, Parcelable {
    companion object {
        /** Sentinel [launchId] used when resuming a running player without starting new playback. */
        const val RESUME_LAUNCH_ID = "audio_player_resume"
    }
}

internal fun EntryProviderScope<NavKey>.audioPlayerScreen(
    navigationHandler: NavigationHandler,
    launchSourceHolder: AudioPlayerLaunchSourceHolder,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<AudioPlayerScreenNavKey> { navKey ->
        val viewModel = hiltViewModel<AudioPlayerViewModel>()

        LaunchedEffect(navKey.launchId) {
            val intent = launchSourceHolder.consume(navKey.launchId) ?: return@LaunchedEffect
            viewModel.startPlayback(intent)
        }

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val nodeSourceType = (uiState as? AudioPlayerUiState.Data)?.nodeSourceType
            ?: NodeSourceType.MEDIA_PLAYER_DEFAULT

        val nodeOptionsActionViewModel =
            hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
                key = nodeSourceType.name,
                creationCallback = { it.create(nodeSourceType) }
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

        val isPodcastMode by viewModel.isPodcastMode.collectAsStateWithLifecycle()

        AudioPlayerScreen(
            uiState = uiState,
            isPodcastMode = isPodcastMode,
            onPlayPauseClicked = viewModel::togglePlayPause,
            onSeekTo = viewModel::seekTo,
            onNextClicked = viewModel::skipToNext,
            onPreviousClicked = viewModel::skipToPrevious,
            onShuffleClicked = viewModel::toggleShuffle,
            onRepeatClicked = viewModel::cycleRepeatMode,
            onPlaylistClicked = { /* TODO: navigate to queue */ },
            onBackPressed = navigationHandler::back,
            onMoreActionsClicked = {
                val navKey = (uiState as? AudioPlayerUiState.Data)?.buildNodeOptionsNavKey()
                    ?: return@AudioPlayerScreen
                navigationHandler.navigate(navKey)
            },
            onToggleMode = viewModel::togglePlayerMode,
            onSeekForward15 = viewModel::seekForward15,
            onSeekBackward15 = viewModel::seekBackward15,
            onSpeedClicked = {},
            onSleepTimerClicked = {},
        )
    }
}

private fun AudioPlayerUiState.Data.buildNodeOptionsNavKey(): DarkNodeOptionsBottomSheetNavKey? {
    val handle = currentPlayingHandle ?: return null
    return DarkNodeOptionsBottomSheetNavKey(
        nodeHandle = handle,
        nodeSourceType = nodeSourceType,
        publicLinkUrl = fileLinkUrl,
        localFilePath = localFilePath,
        chatId = chatId,
        msgId = msgId,
        partiallyExpand = nodeSourceType.shouldPartiallyExpand,
    )
}

private val NodeSourceType.shouldPartiallyExpand: Boolean
    get() = when (this) {
        NodeSourceType.CHAT,
        NodeSourceType.FILE_LINK,
        NodeSourceType.FOLDER_LINK,
        NodeSourceType.MEDIA_PLAYER_VERSIONS,
        NodeSourceType.MEDIA_PLAYER_ZIP_FILE,
        NodeSourceType.MEDIA_PLAYER_IMAGE_VIEWER,
            -> false

        else -> true
    }
