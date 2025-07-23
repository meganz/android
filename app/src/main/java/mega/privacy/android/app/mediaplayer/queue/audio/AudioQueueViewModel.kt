package mega.privacy.android.app.mediaplayer.queue.audio

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.mediaplayer.MediaPlayerActivity.Companion.TYPE_PLAYING
import mega.privacy.android.app.mediaplayer.MediaPlayerActivity.Companion.TYPE_PREVIOUS
import mega.privacy.android.app.mediaplayer.mapper.MediaQueueItemUiEntityMapper
import mega.privacy.android.app.mediaplayer.playlist.PlaylistItem
import mega.privacy.android.app.mediaplayer.queue.model.AudioQueueUiState
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemUiEntity
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.call.IsParticipatingInChatCallUseCase
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

    private var searchQuery: String = ""

    private val originalData = mutableListOf<MediaQueueItemUiEntity>()

    internal fun initMediaQueueItemList(items: List<PlaylistItem>) {
        val queueItems =
            items.convertToMediaQueueItemList().updateOriginalData().filterItemBySearchQuery()
        val playingIndex = queueItems.indexOfFirst { it.type == MediaQueueItemType.Playing }
        _uiState.update {
            it.copy(items = queueItems, indexOfCurrentPlayingItem = playingIndex)
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
                    TYPE_PREVIOUS -> MediaQueueItemType.Previous
                    TYPE_PLAYING -> MediaQueueItemType.Playing
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
        list.updateOriginalData()
        _uiState.update { it.copy(items = list) }
    }

    internal fun updateMediaQueueAfterMediaItemTransition(playingHandle: Long) {
        val items = if (_uiState.value.isSearchMode) {
            originalData
        } else {
            _uiState.value.items
        }
        val index = items.indexOfFirst { playingHandle == it.id.longValue }
        val newItems = items.updateMediaQueueItemType(index).updateOriginalData()
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

    internal fun onItemClicked(index: Int, item: MediaQueueItemUiEntity) =
        updateItemInSelectionState(item = item, index = index)

    private fun updateItemInSelectionState(index: Int, item: MediaQueueItemUiEntity) {
        val isSelected = !item.isSelected
        val selectedHandles = _uiState.value.selectedItemHandles.updateSelectedHandles(
            id = item.id.longValue,
            isSelected = isSelected
        )
        val updateItems =
            _uiState.value.items.updateItemSelectedState(index, isSelected).updateOriginalData()
        _uiState.update {
            it.copy(
                items = updateItems,
                selectedItemHandles = selectedHandles
            )
        }
    }

    private fun List<Long>.updateSelectedHandles(
        id: Long,
        isSelected: Boolean,
    ) = toMutableList().also { handles ->
        if (isSelected) {
            handles.add(id)
        } else {
            handles.remove(id)
        }
    }

    private fun List<MediaQueueItemUiEntity>.updateItemSelectedState(
        index: Int,
        isSelected: Boolean,
    ) =
        if (index in indices) {
            toMutableList().also { list ->
                list[index] = list[index].copy(isSelected = isSelected)
            }
        } else this

    internal fun clearAllSelectedItems() {
        val updatedItems = clearItemsSelected().updateOriginalData().filterItemBySearchQuery()
        _uiState.update {
            it.copy(
                items = updatedItems,
                selectedItemHandles = emptyList()
            )
        }
    }

    private fun clearItemsSelected() = _uiState.value.items.map {
        it.copy(isSelected = false)
    }

    internal fun selectAllNextTypeItems() {
        val updatedItems = allItemsSelected().updateOriginalData().filterItemBySearchQuery()
        val selectedHandles = updatedItems.filter { it.isSelected }.map { it.id.longValue }
        _uiState.update {
            it.copy(
                items = updatedItems,
                selectedItemHandles = selectedHandles
            )
        }
    }

    private fun allItemsSelected() =
        _uiState.value.items.map { item ->
            item.takeIf { it.type == MediaQueueItemType.Next }?.copy(isSelected = true) ?: item
        }

    internal fun removeSelectedItems(
        handles: List<Long> = _uiState.value.selectedItemHandles.toList(),
    ) {
        val currentItems = _uiState.value.items
        val updatedItems = currentItems
            .filterNot { it.id.longValue in handles }
            .updateOriginalData()

        val removedItems = handles.mapNotNull { handle ->
            currentItems.firstOrNull { it.id.longValue == handle }
        }

        _uiState.update {
            it.copy(
                items = updatedItems,
                selectedItemHandles = emptyList(),
                removedItems = removedItems,
                itemsRemovedEvent = triggered
            )
        }
    }

    internal fun clearRemovedItemHandles() =
        _uiState.update { it.copy(removedItems = emptyList()) }

    internal fun updateSearchMode(isSearchMode: Boolean) {
        if (isSearchMode.not()) {
            searchQuery = ""
            searchItemByQueryString()
        }
        _uiState.update {
            it.copy(isSearchMode = isSearchMode)
        }
    }

    internal fun searchQueryUpdate(query: String) {
        searchQuery = query
        searchItemByQueryString()
    }

    private fun searchItemByQueryString() {
        val items = originalData.filter { item ->
            item.nodeName.contains(searchQuery, true)
        }
        _uiState.update {
            it.copy(
                items = items
            )
        }
    }

    internal fun enableSelectMode(isSelectMode: Boolean) =
        _uiState.update {
            it.copy(isSelectMode = isSelectMode)
        }

    internal fun onItemsRemovedEventConsumed() {
        _uiState.update {
            it.copy(itemsRemovedEvent = consumed)
        }
    }
}