package mega.privacy.android.feature.photos.presentation.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.FileNodeContentToNavKeyMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.videosection.GetAllVideosUseCase
import mega.privacy.android.domain.usecase.videosection.GetSyncUploadsFolderIdsUseCase
import mega.privacy.android.feature.photos.mapper.VideoUiEntityMapper
import mega.privacy.android.feature.photos.presentation.videos.model.DurationFilterOption
import mega.privacy.android.feature.photos.presentation.videos.model.LocationFilterOption
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VideosTabViewModel @Inject constructor(
    private val getAllVideosUseCase: GetAllVideosUseCase,
    private val videoUiEntityMapper: VideoUiEntityMapper,
    private val getSyncUploadsFolderIdsUseCase: GetSyncUploadsFolderIdsUseCase,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase,
    private val fileNodeContentToNavKeyMapper: FileNodeContentToNavKeyMapper,
    monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
) : ViewModel() {
    private val triggerFlow = MutableStateFlow(false)
    private val queryFlow = MutableStateFlow<String?>(null)
    private val locationFilterOptionFlow = MutableStateFlow(LocationFilterOption.AllLocations)
    private val durationFilterOptionFlow = MutableStateFlow(DurationFilterOption.AllDurations)
    private val selectedVideoIdsFlow = MutableStateFlow<List<Long>>(emptyList())

    private val navigateToVideoPlayerFlow =
        MutableStateFlow<StateEventWithContent<NavKey>>(consumed())
    internal val navigateToVideoPlayerEvent: StateFlow<StateEventWithContent<NavKey>>
            by lazy(LazyThreadSafetyMode.NONE) {
                navigateToVideoPlayerFlow
            }

    private val dataUpdatedFlow: Flow<VideosTabUiState.Data> =
        combine(
            merge(
                monitorNodeUpdatesUseCase().filter {
                    it.changes.keys.any { node ->
                        node is FileNode && node.type is VideoFileTypeInfo
                    }
                }.catch { Timber.e(it) },
                monitorOfflineNodeUpdatesUseCase().catch { Timber.e(it) }
            ).onStart { emit(Unit) },
            monitorSortCloudOrderUseCase().catch { Timber.e(it) },
            monitorHiddenNodesEnabledUseCase().catch { Timber.e(it) },
            monitorShowHiddenItemsUseCase().catch { Timber.e(it) },
        ) { _, sortOrder, isHiddenNodesEnabled, isShowHiddenItems ->
            val showHiddenItems = isShowHiddenItems || !isHiddenNodesEnabled
            val videoNodes = getVideoNodeList()
                .filterVideosByDuration()
                .filterVideosByLocation()
                .filterNonSensitiveItems(showHiddenItems)
            val uiEntities = videoNodes.map { videoUiEntityMapper(it) }
            val order = sortOrder ?: SortOrder.ORDER_DEFAULT_ASC
            val sortOrderPair = nodeSortConfigurationUiMapper(order)

            VideosTabUiState.Data(
                allVideoEntities = uiEntities,
                allVideoNodes = videoNodes,
                sortOrder = order,
                query = queryFlow.value,
                selectedSortConfiguration = sortOrderPair,
                locationSelectedFilterOption = locationFilterOptionFlow.value,
                durationSelectedFilterOption = durationFilterOptionFlow.value,
                showHiddenItems = showHiddenItems
            )
        }

    internal val uiState: StateFlow<VideosTabUiState> by lazy(LazyThreadSafetyMode.NONE) {
        triggerFlow.flatMapLatest {
            flow {
                emit(VideosTabUiState.Loading)
                emitAll(
                    combine(
                        dataUpdatedFlow,
                        selectedVideoIdsFlow
                    ) { uiState, selectedVideoIds ->
                        val uiEntities = uiState.allVideoEntities.map {
                            it.copy(isSelected = it.id.longValue in selectedVideoIds)
                        }
                        val selectedNodes = uiState.allVideoNodes.filter {
                            it.id.longValue in selectedVideoIds
                        }

                        uiState.copy(
                            allVideoEntities = uiEntities,
                            selectedTypedNodes = selectedNodes
                        )
                    }.catch {
                        Timber.e(it)
                    }
                )
            }
        }.asUiStateFlow(
            viewModelScope,
            VideosTabUiState.Loading
        )
    }

    fun triggerRefresh() {
        triggerFlow.update { !it }
    }

    private suspend fun getVideoNodeList(): List<TypedVideoNode> {
        val query = getCurrentSearchQuery()
        return getAllVideosUseCase(
            searchQuery = query,
            tag = query.removePrefix("#"),
            description = query
        )
    }

    private suspend fun List<TypedVideoNode>.filterVideosByLocation(): List<TypedVideoNode> {
        val syncUploadsFolderIds = getSyncUploadsFolderIdsUseCase()
        return filter {
            when (locationFilterOptionFlow.value) {
                LocationFilterOption.AllLocations -> true
                LocationFilterOption.CloudDrive -> it.parentId.longValue !in syncUploadsFolderIds
                LocationFilterOption.CameraUploads -> it.parentId.longValue in syncUploadsFolderIds
                LocationFilterOption.SharedItems -> it.exportedData != null || it.isOutShared
            }
        }
    }

    private fun List<TypedVideoNode>.filterVideosByDuration() =
        filter {
            val seconds = it.duration.inWholeSeconds
            when (durationFilterOptionFlow.value) {
                DurationFilterOption.AllDurations -> true
                DurationFilterOption.LessThan10Seconds -> seconds < 10
                DurationFilterOption.Between10And60Seconds -> seconds in 10..60
                DurationFilterOption.Between1And4 -> seconds in 61..240
                DurationFilterOption.Between4And20 -> seconds in 241..1200
                DurationFilterOption.MoreThan20 -> seconds > 1200
            }
        }

    private fun List<TypedVideoNode>.filterNonSensitiveItems(showHiddenItems: Boolean) =
        if (showHiddenItems) {
            this
        } else {
            filterNot { it.isMarkedSensitive || it.isSensitiveInherited }
        }

    internal fun setLocationSelectedFilterOption(locationFilterOption: LocationFilterOption) {
        locationFilterOptionFlow.update { locationFilterOption }
        triggerRefresh()
    }

    internal fun setDurationSelectedFilterOption(durationFilterOption: DurationFilterOption) {
        durationFilterOptionFlow.update { durationFilterOption }
        triggerRefresh()
    }

    internal fun getCurrentSearchQuery() = queryFlow.value.orEmpty()

    internal fun searchQuery(queryString: String?) {
        if (queryFlow.value != queryString) {
            queryFlow.update { queryString }
            triggerRefresh()
        }
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

    internal fun onItemClicked(item: VideoUiEntity) {
        if (selectedVideoIdsFlow.value.isNotEmpty()) {
            toggleItemSelection(item)
        } else {
            navigateToVideoPlayer(item)
        }
    }

    private fun navigateToVideoPlayer(item: VideoUiEntity) {
        viewModelScope.launch {
            val state = uiState.value as? VideosTabUiState.Data ?: return@launch
            val videoNode = state.allVideoNodes.firstOrNull { it.id == item.id } ?: return@launch
            val content = FileNodeContent.AudioOrVideo(uri = getNodeContentUriUseCase(videoNode))

            val isSearchMode = !state.query.isNullOrEmpty()
            fileNodeContentToNavKeyMapper(
                content = content,
                fileNode = videoNode,
                nodeSourceType = if (isSearchMode) {
                    NodeSourceType.SEARCH
                } else {
                    NodeSourceType.VIDEOS
                },
                sortOrder = state.sortOrder,
                searchedItems = if (isSearchMode) {
                    state.allVideoNodes.map { it.id.longValue }
                } else {
                    null
                }
            )?.let { navKey ->
                navigateToVideoPlayerFlow.update { triggered(navKey) }
            }
        }
    }

    internal fun resetNavigateToVideoPlayer() {
        navigateToVideoPlayerFlow.update { consumed() }
    }

    internal fun onItemLongClicked(item: VideoUiEntity) {
        toggleItemSelection(item = item)
    }

    private fun toggleItemSelection(item: VideoUiEntity) {
        val updatedSelectedIds = selectedVideoIdsFlow.value.toMutableList()
        if (item.id.longValue in updatedSelectedIds) {
            updatedSelectedIds -= item.id.longValue
        } else {
            updatedSelectedIds += item.id.longValue
        }
        selectedVideoIdsFlow.update { updatedSelectedIds }
    }

    internal fun selectAllVideos() {
        val state = uiState.value as? VideosTabUiState.Data ?: return
        val allIds = state.allVideoEntities.map { it.id.longValue }
        selectedVideoIdsFlow.update { allIds }
    }

    internal fun clearSelection() {
        selectedVideoIdsFlow.update { emptyList() }
    }
}