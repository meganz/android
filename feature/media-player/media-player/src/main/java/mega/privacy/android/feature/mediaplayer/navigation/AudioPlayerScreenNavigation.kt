package mega.privacy.android.feature.mediaplayer.navigation

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberSingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.sheet.options.DarkNodeOptionsBottomSheetNavKey
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.mediaplayer.components.PlaybackSpeedBottomSheet
import mega.privacy.android.feature.mediaplayer.presentation.AudioPlayerQueueScreen
import mega.privacy.android.feature.mediaplayer.presentation.AudioPlayerQueueViewModel
import mega.privacy.android.feature.mediaplayer.presentation.AudioPlayerScreen
import mega.privacy.android.feature.mediaplayer.presentation.AudioPlayerViewModel
import mega.privacy.android.feature.mediaplayer.presentation.SleepTimerBottomSheet
import mega.privacy.android.feature.mediaplayer.presentation.model.AudioPlayerUiState
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.transition.slideDownBackwardTransition
import mega.privacy.android.navigation.contract.transition.slideUpForwardTransition

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

/** Navigation key for the audio player play-queue screen. */
@Serializable
@Parcelize
data object AudioPlayerQueueScreenNavKey : NavKey, Parcelable

internal fun EntryProviderScope<NavKey>.audioPlayerScreen(
    navigationHandler: NavigationHandler,
    launchSourceHolder: AudioPlayerLaunchSourceHolder,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<AudioPlayerQueueScreenNavKey>(
        metadata = NavDisplay.transitionSpec { slideUpForwardTransition } +
                NavDisplay.popTransitionSpec { slideDownBackwardTransition } +
                NavDisplay.predictivePopTransitionSpec { slideDownBackwardTransition }
    ) {
        val activity = LocalActivity.current as? ComponentActivity
            ?: error("AudioPlayerQueueScreen must be hosted in a ComponentActivity")
        val playerViewModel = hiltViewModel<AudioPlayerViewModel>(activity)
        val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()

        val viewModel = hiltViewModel<AudioPlayerQueueViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        HandleAudioPlayerNodeOptions(
            uiState = playerUiState,
            navigationHandler = navigationHandler,
            onTransfer = onTransfer,
        )

        AudioPlayerQueueScreen(
            uiState = uiState,
            onBack = navigationHandler::back,
            onQueueItemClick = viewModel::seekToQueueItem,
            onSetContinuousPlayback = viewModel::setContinuousPlayback,
            onMoreActionsClicked = {
                (playerUiState as? AudioPlayerUiState.Data)?.buildNodeOptionsNavKey()
                    ?.let(navigationHandler::navigate)
            },
        )
    }

    entry<AudioPlayerScreenNavKey> { navKey ->
        val activity = LocalActivity.current as? ComponentActivity
            ?: error("AudioPlayerScreen must be hosted in a ComponentActivity")
        val viewModel = hiltViewModel<AudioPlayerViewModel>(activity)

        LaunchedEffect(navKey.launchId) {
            val intent = launchSourceHolder.consume(navKey.launchId) ?: return@LaunchedEffect
            viewModel.startPlayback(intent)
        }

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        HandleAudioPlayerNodeOptions(
            uiState = uiState,
            navigationHandler = navigationHandler,
            onTransfer = onTransfer,
        )

        val isPodcastMode by viewModel.isPodcastMode.collectAsStateWithLifecycle()
        val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
        var showSpeedSheet by remember { mutableStateOf(false) }
        var showSleepTimerSheet by remember { mutableStateOf(false) }

        AudioPlayerScreen(
            uiState = uiState,
            isPodcastMode = isPodcastMode,
            onPlayPauseClicked = viewModel::togglePlayPause,
            onSeekTo = viewModel::seekTo,
            onNextClicked = viewModel::skipToNext,
            onPreviousClicked = viewModel::skipToPrevious,
            onShuffleClicked = viewModel::toggleShuffle,
            onRepeatClicked = viewModel::cycleRepeatMode,
            onPlaylistClicked = { navigationHandler.navigate(AudioPlayerQueueScreenNavKey) },
            onBackPressed = navigationHandler::back,
            onMoreActionsClicked = {
                val navKey = (uiState as? AudioPlayerUiState.Data)?.buildNodeOptionsNavKey()
                    ?: return@AudioPlayerScreen
                navigationHandler.navigate(navKey)
            },
            onToggleMode = viewModel::togglePlayerMode,
            onSeekForward15 = viewModel::seekForward15,
            onSeekBackward15 = viewModel::seekBackward15,
            onSpeedClicked = { showSpeedSheet = true },
            onSleepTimerClicked = { showSleepTimerSheet = true },
            sleepTimerState = sleepTimerState,
        )

        if (showSleepTimerSheet) {
            SleepTimerBottomSheet(
                sleepTimerState = sleepTimerState,
                onOptionSelected = { option ->
                    viewModel.setSleepTimer(option)
                    showSleepTimerSheet = false
                },
                onTurnOff = {
                    viewModel.cancelSleepTimer()
                    showSleepTimerSheet = false
                },
                onDismiss = { showSleepTimerSheet = false },
            )
        }

        if (showSpeedSheet) {
            val currentSpeed = (uiState as? AudioPlayerUiState.Data)?.currentPlaybackSpeed ?: 1f
            PlaybackSpeedBottomSheet(
                currentSpeed = currentSpeed,
                isDark = true, // audio player UI is always dark-themed
                onSpeedSelected = { speed ->
                    viewModel.setPlaybackSpeed(speed)
                    showSpeedSheet = false
                },
                onDismiss = { showSpeedSheet = false },
            )
        }
    }
}

/**
 * Wires up the node-options bottom-sheet result handling for an audio player entry.
 *
 * Shared by the player and queue entries so that a sheet opened from either screen has its
 * selected action handled by the entry beneath it.
 */
@Composable
private fun HandleAudioPlayerNodeOptions(
    uiState: AudioPlayerUiState,
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
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
