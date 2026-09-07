package mega.privacy.android.feature.mediaplayer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.MonitorAudioContinuousPlaybackEnabledUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioContinuousPlaybackEnabledUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.feature.mediaplayer.data.gateway.AudioMediaControllerGateway
import mega.privacy.android.feature.mediaplayer.presentation.model.AudioPlayerQueueUiState
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
    private val monitorAudioContinuousPlaybackEnabledUseCase: MonitorAudioContinuousPlaybackEnabledUseCase,
    private val setAudioContinuousPlaybackEnabledUseCase: SetAudioContinuousPlaybackEnabledUseCase,
) : ViewModel() {

    val uiState: StateFlow<AudioPlayerQueueUiState> by lazy(LazyThreadSafetyMode.NONE) {
        val playingFromNameFlow = gateway.playerState
            .map { it.currentMediaItemHandle }
            .distinctUntilChanged()
            .flatMapLatest { handle ->
                if (handle == null) {
                    flowOf<String?>(null)
                } else {
                    flow {
                        emit(null)
                        emit(
                            runCatching {
                                val node = getNodeByHandleUseCase(handle) ?: return@runCatching null
                                getNodeByHandleUseCase(node.parentId.longValue)?.name
                            }.onFailure {
                                Timber.w(it, "Failed to fetch parent name for handle=$handle")
                            }.getOrNull()
                        )
                    }
                }
            }
            .catch {
                Timber.e(it, "Failed to fetch playing-from name")
                emit(null)
            }

        val continuousPlaybackFlow = monitorAudioContinuousPlaybackEnabledUseCase()
            .catch {
                Timber.e(it, "Failed to monitor continuous playback preference")
                emit(true)
            }

        combine(
            gateway.playerState
                .catch { Timber.e(it, "Failed to collect player state in queue ViewModel") },
            playingFromNameFlow,
            continuousPlaybackFlow,
        ) { state, playingFromName, continuousPlaybackEnabled ->
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
                playingFromName = playingFromName,
            )
        }.asUiStateFlow(
            viewModelScope,
            AudioPlayerQueueUiState.Loading,
        )
    }

    /** Seeks to the queue item at [index] and begins playback from its start. */
    fun seekToQueueItem(index: Int) {
        gateway.seekToMediaItem(index)
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
