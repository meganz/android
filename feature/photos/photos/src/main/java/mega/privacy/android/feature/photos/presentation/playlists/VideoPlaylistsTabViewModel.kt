package mega.privacy.android.feature.photos.presentation.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistsUseCase
import mega.privacy.android.domain.usecase.videosection.MonitorVideoPlaylistSetsUpdateUseCase
import mega.privacy.android.domain.usecase.videosection.RemoveVideoPlaylistsUseCase
import mega.privacy.android.feature.photos.mapper.VideoPlaylistUiEntityMapper
import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VideoPlaylistsTabViewModel @Inject constructor(
    private val getVideoPlaylistsUseCase: GetVideoPlaylistsUseCase,
    private val videoPlaylistUiEntityMapper: VideoPlaylistUiEntityMapper,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val removeVideoPlaylistsUseCase: RemoveVideoPlaylistsUseCase,
    monitorVideoPlaylistSetsUpdateUseCase: MonitorVideoPlaylistSetsUpdateUseCase,
    monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
) : ViewModel() {
    private val triggerFlow = MutableStateFlow(false)
    private val selectedVideoPlaylistsFlow =
        MutableStateFlow<Set<VideoPlaylistUiEntity>>(emptySet())
    private val playlistsRemovedFlow =
        MutableStateFlow<StateEventWithContent<List<String>>>(consumed())

    internal val uiState: StateFlow<VideoPlaylistsTabUiState> by lazy {
        triggerFlow.flatMapLatest {
            combine(
                merge(
                    monitorVideoPlaylistSetsUpdateUseCase(),
                    monitorNodeUpdatesUseCase().filter {
                        it.changes.keys.any { node ->
                            node is FileNode && node.type is VideoFileTypeInfo
                        }
                    },
                    selectedVideoPlaylistsFlow,
                    playlistsRemovedFlow,
                ).onStart { emit(Unit) },
                monitorSortCloudOrderUseCase(),
            ) { _, sortOrder ->
                val videoPlaylists = getVideoPlaylistsUseCase()
                val selectedPlaylists = selectedVideoPlaylistsFlow.value
                val selectedIds = selectedPlaylists.map { playlist -> playlist.id }
                val videoPlaylistEntities = videoPlaylists.map {
                    val entity = videoPlaylistUiEntityMapper(it)
                    entity.copy(
                        isSelected = entity.id in selectedIds
                    )
                }

                val convertedSortOrder =
                    sortOrder?.convertPlaylistSortOrder() ?: SortOrder.ORDER_DEFAULT_ASC
                val sortOrderPair = nodeSortConfigurationUiMapper(convertedSortOrder)

                VideoPlaylistsTabUiState.Data(
                    videoPlaylists = videoPlaylists,
                    videoPlaylistEntities = videoPlaylistEntities,
                    sortOrder = convertedSortOrder,
                    selectedSortConfiguration = sortOrderPair,
                    selectedPlaylists = selectedPlaylists,
                    playlistsRemovedEvent = playlistsRemovedFlow.value
                )
            }.catch {
                Timber.e(it)
            }
        }.asUiStateFlow(
            viewModelScope,
            VideoPlaylistsTabUiState.Loading
        )
    }

    private fun SortOrder.convertPlaylistSortOrder() =
        when (this) {
            SortOrder.ORDER_DEFAULT_DESC -> SortOrder.ORDER_DEFAULT_DESC
            SortOrder.ORDER_CREATION_ASC -> SortOrder.ORDER_CREATION_ASC
            SortOrder.ORDER_CREATION_DESC -> SortOrder.ORDER_CREATION_DESC
            SortOrder.ORDER_MODIFICATION_ASC -> SortOrder.ORDER_MODIFICATION_ASC
            SortOrder.ORDER_MODIFICATION_DESC -> SortOrder.ORDER_MODIFICATION_DESC
            else -> SortOrder.ORDER_DEFAULT_ASC
        }

    fun triggerRefresh() {
        triggerFlow.update { !it }
    }

    internal fun setCloudSortOrder(sortConfiguration: NodeSortConfiguration) {
        viewModelScope.launch {
            runCatching {
                val order = nodeSortConfigurationUiMapper(sortConfiguration)
                setCloudSortOrderUseCase(order)
            }.onFailure {
                Timber.e(it, "Failed to set cloud sort order")
            }
        }
    }

    internal fun onItemClicked(item: VideoPlaylistUiEntity) {
        if (selectedVideoPlaylistsFlow.value.isNotEmpty() && !item.isSystemVideoPlayer) {
            toggleItemSelection(item)
        }
    }

    internal fun onItemLongClicked(item: VideoPlaylistUiEntity) {
        if (!item.isSystemVideoPlayer) {
            toggleItemSelection(item = item)
        }
    }

    private fun toggleItemSelection(item: VideoPlaylistUiEntity) {
        val updatedSelectedPlaylists =
            selectedVideoPlaylistsFlow.value.toMutableSet()
        val existingItem = updatedSelectedPlaylists.firstOrNull { it.id == item.id }
        if (existingItem == null) {
            updatedSelectedPlaylists += item
        } else {
            updatedSelectedPlaylists -= existingItem
        }
        selectedVideoPlaylistsFlow.update { updatedSelectedPlaylists }
    }

    internal fun selectAllVideos() {
        if (uiState.value is VideoPlaylistsTabUiState.Data) {
            val allPlaylists =
                (uiState.value as VideoPlaylistsTabUiState.Data).videoPlaylistEntities.toSet()
            selectedVideoPlaylistsFlow.update { allPlaylists }
        }
    }

    internal fun clearSelection() {
        selectedVideoPlaylistsFlow.update { emptySet() }
    }

    internal fun removeVideoPlaylists(deletedItems: Set<VideoPlaylistUiEntity>) =
        viewModelScope.launch {
            runCatching {
                val ids = deletedItems.map { it.id }
                removeVideoPlaylistsUseCase(ids)
            }.onSuccess { deletedPlaylistIDs ->
                val deletedTitlesById = deletedItems.associate {
                    it.id.longValue to it.title
                }
                val deletedPlaylistTitles = deletedPlaylistIDs.mapNotNull { id ->
                    deletedTitlesById[id]
                }
                Timber.d("removeVideoPlaylists deletedPlaylistTitles: $deletedPlaylistTitles")
                playlistsRemovedFlow.update { triggered(deletedPlaylistTitles) }
                clearSelection()
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    internal fun resetPlaylistsRemovedEvent() {
        playlistsRemovedFlow.update { consumed() }
    }
}