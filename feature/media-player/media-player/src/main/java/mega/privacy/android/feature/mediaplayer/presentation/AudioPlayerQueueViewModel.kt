package mega.privacy.android.feature.mediaplayer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
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
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
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

        combine(
            gateway.playerState
                .catch { Timber.e(it, "Failed to collect player state in queue ViewModel") },
            playingFromNameFlow,
        ) { state, playingFromName ->
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
                isContinuousPlayback = state.repeatMode != Player.REPEAT_MODE_OFF,
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

    /** Sets continuous playback. When [enabled], the queue loops indefinitely; otherwise it stops after the last item. */
    fun setContinuousPlayback(enabled: Boolean) {
        gateway.setRepeatMode(if (enabled) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF)
    }
}
