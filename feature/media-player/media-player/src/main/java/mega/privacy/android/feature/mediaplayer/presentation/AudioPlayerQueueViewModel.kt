package mega.privacy.android.feature.mediaplayer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.call.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.MonitorAudioContinuousPlaybackEnabledUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioContinuousPlaybackEnabledUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.feature.mediaplayer.data.gateway.AudioMediaControllerGateway
import mega.privacy.android.feature.mediaplayer.presentation.model.AudioPlayerQueueUiState
import mega.privacy.android.feature.mediaplayer.presentation.model.PlayingFromSection
import timber.log.Timber

/**
 * ViewModel for the audio player play-queue screen.
 *
 * Observes player state changes and exposes them through [uiState].
 * Playback commands are forwarded to the gateway without analytics or persistence
 * side effects (the main [AudioPlayerViewModel] handles those).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class AudioPlayerQueueViewModel @Inject constructor(
    private val gateway: AudioMediaControllerGateway,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase,
    private val monitorAudioContinuousPlaybackEnabledUseCase: MonitorAudioContinuousPlaybackEnabledUseCase,
    private val setAudioContinuousPlaybackEnabledUseCase: SetAudioContinuousPlaybackEnabledUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase,
) : ViewModel() {

    /** Where the queue is playing from: a named folder, or a root section the UI localizes. */
    private data class PlayingFrom(
        val name: String? = null,
        val section: PlayingFromSection? = null,
    )

    /**
     * Triggered when starting a queue item was refused because a MEGA call is ongoing; carried
     * inside [uiState] and consumed via [onControlBlockedByCallConsumed].
     */
    private val controlBlockedByCallChannel = Channel<StateEvent>(Channel.BUFFERED)

    /** Marks the blocked-by-call announcement as shown. */
    fun onControlBlockedByCallConsumed() {
        controlBlockedByCallChannel.trySend(consumed)
    }

    val uiState: StateFlow<AudioPlayerQueueUiState> by lazy(LazyThreadSafetyMode.NONE) {
        val playingFromFlow = gateway.playerState
            .map { it.currentMediaItemHandle }
            .distinctUntilChanged()
            .flatMapLatest { handle ->
                if (handle == null) {
                    flowOf(PlayingFrom())
                } else {
                    flow {
                        emit(PlayingFrom())
                        emit(
                            runCatching { resolvePlayingFrom(handle) }
                                .onFailure {
                                    Timber.w(it, "Failed to fetch parent name for handle=$handle")
                                }
                                .getOrDefault(PlayingFrom())
                        )
                    }
                }
            }
            .catch {
                Timber.e(it, "Failed to fetch playing-from name")
                emit(PlayingFrom())
            }

        val continuousPlaybackFlow = monitorAudioContinuousPlaybackEnabledUseCase()
            .catch {
                Timber.e(it, "Failed to monitor continuous playback preference")
                emit(true)
            }

        combine(
            gateway.playerState
                .catch { Timber.e(it, "Failed to collect player state in queue ViewModel") },
            playingFromFlow,
            continuousPlaybackFlow,
            controlBlockedByCallChannel.receiveAsFlow()
                .onStart { emit(consumed) },
        ) { state, playingFrom, continuousPlaybackEnabled, controlBlockedByCall ->
            val enrichedItems = state.queueItems.mapIndexed { index, item ->
                if (index == state.currentQueueIndex) {
                    item.copy(
                        title = state.title ?: item.title,
                        artist = state.artist ?: item.artist,
                    )
                } else {
                    item
                }
            }
            AudioPlayerQueueUiState.Data(
                items = enrichedItems,
                currentQueueIndex = state.currentQueueIndex,
                currentTitle = state.title,
                currentArtist = state.artist,
                currentArtworkUri = state.artworkUri,
                currentThumbnailData = state.currentMediaItemHandle?.let {
                    ThumbnailRequest.fromHandle(it)
                },
                isContinuousPlayback = continuousPlaybackEnabled,
                isShuffleEnabled = state.shuffleEnabled,
                playingFromName = playingFrom.name,
                playingFromSection = playingFrom.section,
                controlBlockedByCall = controlBlockedByCall,
            )
        }.asUiStateFlow(
            viewModelScope,
            AudioPlayerQueueUiState.Loading,
        )
    }

    private suspend fun resolvePlayingFrom(handle: Long): PlayingFrom {
        val node = getNodeByHandleUseCase(handle) ?: return PlayingFrom()
        val parentHandle = node.parentId.longValue
        rootSectionOf(parentHandle)?.let { return PlayingFrom(section = it) }
        return PlayingFrom(name = getNodeByHandleUseCase(parentHandle)?.name)
    }

    /**
     * The SDK's root nodes carry internal names, so a track sitting directly in Cloud Drive or
     * the Rubbish bin is reported as its section and localized by the UI.
     */
    private suspend fun rootSectionOf(parentHandle: Long): PlayingFromSection? = when (parentHandle) {
        rootHandle { getRootNodeUseCase()?.id?.longValue } -> PlayingFromSection.CloudDrive
        rootHandle { getRubbishNodeUseCase()?.id?.longValue } -> PlayingFromSection.RubbishBin
        else -> null
    }

    private suspend fun rootHandle(fetch: suspend () -> Long?): Long? =
        runCatching { fetch() }
            .onFailure { Timber.w(it, "Failed to resolve a root node handle") }
            .getOrNull()

    /** Seeks to the queue item at player [index] ([mega.privacy.android.feature.mediaplayer.data.model.AudioQueueItem.playerIndex])
     * and begins playback from its start. Refused while a MEGA call is ongoing, since starting
     * playback would grab audio focus from the call. */
    fun seekToQueueItem(index: Int) {
        viewModelScope.launch {
            val inCall = runCatching { isParticipatingInChatCallUseCase() }
                .onFailure { Timber.e(it, "Failed to check ongoing call state") }
                .getOrDefault(false)
            if (inCall) {
                controlBlockedByCallChannel.send(triggered)
            } else {
                gateway.seekToMediaItem(index)
            }
        }
    }

    /**
     * Sets continuous playback. When [enabled], playback moves on to the next queue item;
     * otherwise it pauses once the current item ends -- unless a repeat mode is keeping it going.
     *
     * Only the preference is written: the player service observes it and applies the result.
     */
    fun setContinuousPlayback(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { setAudioContinuousPlaybackEnabledUseCase(enabled) }
                .onFailure { Timber.e(it, "Failed to save continuous playback preference") }
        }
    }

    /** Moves the queue item at [fromIndex] to [toIndex], keeping playback of the current item uninterrupted. */
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        gateway.moveMediaItem(fromIndex, toIndex)
    }

    override fun onCleared() {
        // The gateway binding is unscoped, so this ViewModel owns its own MediaController
        // connection; without releasing it here every opened queue leaks a service binding
        // whose listener keeps position polling alive for as long as playback continues.
        gateway.release()
        super.onCleared()
    }
}
