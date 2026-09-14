package mega.privacy.android.feature.mediaplayer.navigation

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.consumed
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.android.core.ui.model.SnackbarAttributes
import mega.android.core.ui.model.SnackbarDuration
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberSingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.sheet.options.DarkNodeOptionsBottomSheetNavKey
import mega.privacy.android.core.nodecomponents.sheet.options.HandleNodeOptionsActionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.mediaplayer.components.PlaybackSpeedBottomSheet
import mega.privacy.android.feature.mediaplayer.presentation.AudioPlayerErrorDialog
import mega.privacy.android.feature.mediaplayer.presentation.AudioPlayerQueueContent
import mega.privacy.android.feature.mediaplayer.presentation.AudioPlayerQueueViewModel
import mega.privacy.android.feature.mediaplayer.presentation.AudioPlayerScreen
import mega.privacy.android.feature.mediaplayer.presentation.AudioPlayerViewModel
import mega.privacy.android.feature.mediaplayer.presentation.SleepTimerBottomSheet
import mega.privacy.android.feature.mediaplayer.presentation.model.AudioPlayerQueueUiState
import mega.privacy.android.feature.mediaplayer.presentation.model.AudioPlayerUiState
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.shared.resources.R as sharedR

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

/** How long a restored entry without a launch intent waits for a running session to report in. */
private val ACTIVE_SESSION_TIMEOUT = 3.seconds

internal fun EntryProviderScope<NavKey>.audioPlayerScreen(
    navigationHandler: NavigationHandler,
    launchSourceHolder: AudioPlayerLaunchSourceHolder,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<AudioPlayerScreenNavKey> { navKey ->
        val activity = LocalActivity.current as? ComponentActivity
            ?: error("AudioPlayerScreen must be hosted in a ComponentActivity")
        val viewModel = hiltViewModel<AudioPlayerViewModel>(activity)

        LaunchedEffect(navKey.launchId) {
            val intent = launchSourceHolder.consume(navKey.launchId)
            when {
                intent != null -> viewModel.startPlayback(intent)

                // A configuration change also re-runs this effect with the payload already
                // consumed; the ViewModel survived it and playback may simply still be loading,
                // so the restore timeout below must not apply.
                viewModel.hasStartedPlayback -> Unit

                else -> {
                    // Restored after process death: the launch payload is gone. Give the media
                    // controller a short window to prove a session is still running (the service
                    // may have survived the UI process); otherwise leave the now-empty screen.
                    val hasSession = withTimeoutOrNull(ACTIVE_SESSION_TIMEOUT) {
                        viewModel.hasActiveSession.first { it }
                    }
                    if (hasSession == null) navigationHandler.back()
                }
            }
        }

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        HandleAudioPlayerNodeOptions(
            uiState = uiState,
            navigationHandler = navigationHandler,
            onTransfer = onTransfer,
        )

        val isPodcastMode by viewModel.isPodcastMode.collectAsStateWithLifecycle()
        val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()

        val resumedFromPosition by viewModel.resumedFromPosition.collectAsStateWithLifecycle()
        val snackBarQueue = rememberSnackBarQueue()
        val context = LocalContext.current
        LaunchedEffect(resumedFromPosition) {
            val position = resumedFromPosition ?: return@LaunchedEffect
            snackBarQueue.queueMessage(
                SnackbarAttributes(
                    message = context.getString(
                        sharedR.string.audio_player_resumed_snackbar_message,
                        position,
                    ),
                    // Short is ~4s; the default for an action snackbar would be ~10s.
                    duration = SnackbarDuration.Short,
                    action = context.getString(
                        sharedR.string.audio_playback_position_dialog_restart_button
                    ),
                    actionClick = viewModel::restartFromBeginning,
                )
            )
            viewModel.onResumeEventConsumed()
        }
        val playbackError by viewModel.playbackError.collectAsStateWithLifecycle()
        playbackError?.let { error ->
            AudioPlayerErrorDialog(
                error = error,
                onDismiss = {
                    viewModel.onPlaybackErrorConsumed()
                    viewModel.stopPlayer()
                    navigationHandler.back()
                },
            )
        }

        val controlBlockedByCall by viewModel.controlBlockedByCall.collectAsStateWithLifecycle()
        EventEffect(
            event = controlBlockedByCall,
            onConsumed = viewModel::onControlBlockedByCallConsumed,
        ) {
            snackBarQueue.queueMessage(
                SnackbarAttributes(
                    message = context.getString(
                        sharedR.string.audio_player_not_allowed_during_call
                    ),
                    duration = SnackbarDuration.Short,
                )
            )
        }

        val sessionEnded by viewModel.sessionEnded.collectAsStateWithLifecycle()
        LaunchedEffect(sessionEnded) {
            // The flow is the authority, not the composed snapshot: a session that ended while
            // no screen was attached (mini player closed) leaves a stale true behind, and
            // startPlayback clears the flow before this effect runs — but the Compose state
            // lags one collection hop, so acting on the snapshot alone would pop a freshly
            // opened player right back off the stack.
            if (sessionEnded && viewModel.sessionEnded.value) {
                viewModel.onSessionEndedConsumed()
                // A parked error owns the exit: its dialog's OK button navigates back itself.
                if (viewModel.playbackError.value == null) navigationHandler.back()
            }
        }

        var showSpeedSheet by rememberSaveable { mutableStateOf(false) }
        var showSleepTimerSheet by rememberSaveable { mutableStateOf(false) }

        AudioPlayerScreen(
            uiState = uiState,
            isPodcastMode = isPodcastMode,
            onPlayPauseClicked = viewModel::togglePlayPause,
            onSeekTo = viewModel::seekTo,
            onNextClicked = viewModel::skipToNext,
            onPreviousClicked = viewModel::skipToPrevious,
            onShuffleClicked = viewModel::toggleShuffle,
            onRepeatClicked = viewModel::cycleRepeatMode,
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
            queueContent = { queueModifier -> AudioPlayerQueue(modifier = queueModifier) },
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
 * Play queue, rendered by the player as a landscape pane or a portrait overlay.
 *
 * Only composed while the queue is visible, so [AudioPlayerQueueViewModel] is never created for a
 * player entry whose queue is never opened. Once opened, the ViewModel is retained for the rest of
 * the entry — it resolves against the entry's ViewModelStoreOwner — but its player-state
 * subscription is not: `uiState` is shared with `WhileSubscribed`, so collection stops shortly
 * after the queue leaves composition and resumes when it is shown again.
 */
@Composable
private fun AudioPlayerQueue(modifier: Modifier = Modifier) {
    val viewModel = hiltViewModel<AudioPlayerQueueViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackBarQueue = rememberSnackBarQueue()
    val context = LocalContext.current
    EventEffect(
        event = (uiState as? AudioPlayerQueueUiState.Data)?.controlBlockedByCall ?: consumed,
        onConsumed = viewModel::onControlBlockedByCallConsumed,
    ) {
        snackBarQueue.queueMessage(
            SnackbarAttributes(
                message = context.getString(
                    sharedR.string.audio_player_not_allowed_during_call
                ),
                duration = SnackbarDuration.Short,
            )
        )
    }

    AudioPlayerQueueContent(
        uiState = uiState,
        onQueueItemClick = viewModel::seekToQueueItem,
        onQueueItemMoved = viewModel::moveQueueItem,
        onSetContinuousPlayback = viewModel::setContinuousPlayback,
        modifier = modifier,
    )
}

/**
 * Wires up the node-options bottom-sheet result handling for the audio player entry, covering
 * sheets opened from the player and from the play queue alike.
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
