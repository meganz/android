package mega.privacy.android.app.mediaplayer.queue.video

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.mapper.MediaQueueItemUiEntityMapper
import mega.privacy.android.app.mediaplayer.playlist.PlaylistAdapter
import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemUiEntity
import mega.privacy.android.app.mediaplayer.queue.model.VideoQueueUiState
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.meeting.IsParticipatingInChatCallUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Video Queue view model
 */
@HiltViewModel
class VideoQueueViewModel @Inject constructor(
    @VideoPlayer private val mediaPlayerGateway: MediaPlayerGateway,
    private val mediaQueueItemUiEntityMapper: MediaQueueItemUiEntityMapper,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    private val isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VideoQueueUiState())

    internal val uiState = _uiState.asStateFlow()

    private var searchQuery: String = ""

    private val originalData = mutableListOf<MediaQueueItemUiEntity>()

    internal fun initMediaQueueItemList(items: List<PlaylistItem>) {
        val queueItems =
            items.convertToMediaQueueItemList().updateOriginalData().filterItemBySearchQuery()
        val playingIndex = queueItems.indexOfFirst { it.type == MediaQueueItemType.Playing }
        val currentPlayingPosition = mediaPlayerGateway.getCurrentPlayingPosition()
            .formatToString(durationInSecondsTextMapper)
        _uiState.update {
            it.copy(
                items = queueItems,
                indexOfCurrentPlayingItem = playingIndex,
                currentPlayingPosition = currentPlayingPosition
            )
        }
    }

    private fun List<MediaQueueItemUiEntity>.updateOriginalData() = also { data ->
        if (originalData.isNotEmpty()) {
            originalData.clear()
        }
        originalData.addAll(data)
    }

    private fun List<MediaQueueItemUiEntity>.filterItemBySearchQuery() =
        filter { item ->
            item.nodeName.contains(searchQuery, true)
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

    private fun Long.formatToString(durationInSecondsTextMapper: DurationInSecondsTextMapper) =
        durationInSecondsTextMapper(this.milliseconds)

    internal fun updateMediaQueueAfterReorder(from: Int, to: Int) {
        val list = uiState.value.items.toMutableList()
        list.add(to, list.removeAt(from))
        list.updateOriginalData()
        _uiState.update { it.copy(items = list) }
    }

    internal suspend fun isParticipatingInChatCall() = isParticipatingInChatCallUseCase()

    internal fun seekTo(index: Int) = mediaPlayerGateway.playerSeekTo(index)
}