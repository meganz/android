package mega.privacy.android.app.mediaplayer.queue.audio

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.mediaplayer.mapper.MediaQueueItemUiEntityMapper
import mega.privacy.android.app.mediaplayer.playlist.PlaylistAdapter
import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import mega.privacy.android.app.mediaplayer.queue.model.AudioQueueUiState
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemUiEntity
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.meeting.IsParticipatingInChatCallUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Audio Queue view model
 */
@HiltViewModel
class AudioQueueViewModel @Inject constructor(
    private val mediaQueueItemUiEntityMapper: MediaQueueItemUiEntityMapper,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    private val isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AudioQueueUiState(emptyList()))

    internal val uiState = _uiState.asStateFlow()

    internal fun initMediaQueueItemList(items: List<PlaylistItem>) {
        val queueItems = items.convertToMediaQueueItemList()
        val playingIndex = queueItems.indexOfFirst { it.type == MediaQueueItemType.Playing }
        _uiState.update {
            it.copy(items = queueItems, indexOfCurrentPlayingItem = playingIndex)
        }
    }

    private fun List<PlaylistItem>.convertToMediaQueueItemList() =
        map { item ->
            mediaQueueItemUiEntityMapper(
                item.icon,
                item.thumbnail,
                NodeId(item.nodeHandle),
                item.nodeName,
                when (item.type) {
                    PlaylistAdapter.TYPE_PREVIOUS -> MediaQueueItemType.Previous
                    PlaylistAdapter.TYPE_PLAYING -> MediaQueueItemType.Playing
                    else -> MediaQueueItemType.Next
                },
                item.duration,
            )
        }

    internal fun updatePlaybackState(isPaused: Boolean) =
        _uiState.update {
            it.copy(isPaused = isPaused)
        }

    internal fun updateCurrentPlayingPosition(currentPlayingPosition: Long) =
        _uiState.update {
            it.copy(
                currentPlayingPosition = currentPlayingPosition.formatToString(
                    durationInSecondsTextMapper
                )
            )
        }

    private fun Long.formatToString(durationInSecondsTextMapper: DurationInSecondsTextMapper) =
        durationInSecondsTextMapper(this.milliseconds)

    internal fun updateMediaQueueAfterReorder(from: Int, to: Int) {
        val list = uiState.value.items.toMutableList()
        list.add(to, list.removeAt(from))
        _uiState.update { it.copy(items = list) }
    }

    internal fun updateMediaQueueAfterMediaItemTransition(playingHandle: Long) {
        val index = uiState.value.items.indexOfFirst { playingHandle == it.id.longValue }
        val newItems = uiState.value.items.updateMediaQueueItemType(index)
        val playingIndex = newItems.indexOfFirst { it.type == MediaQueueItemType.Playing }
        _uiState.update { it.copy(items = newItems, indexOfCurrentPlayingItem = playingIndex) }
    }

    private fun List<MediaQueueItemUiEntity>.updateMediaQueueItemType(playingIndex: Int) =
        if (playingIndex in indices) {
            toMutableList().also { list ->
                list.forEachIndexed { index, item ->
                    list[index] = item.copy(
                        type = when {
                            index < playingIndex -> MediaQueueItemType.Previous
                            index > playingIndex -> MediaQueueItemType.Next
                            else -> MediaQueueItemType.Playing
                        }
                    )
                }
            }
        } else this

    internal suspend fun isParticipatingInChatCall() = isParticipatingInChatCallUseCase()
}