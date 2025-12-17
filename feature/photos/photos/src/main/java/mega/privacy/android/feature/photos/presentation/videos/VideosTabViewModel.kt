package mega.privacy.android.feature.photos.presentation.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.SetCloudSortOrder
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
    private val getCloudSortOrder: GetCloudSortOrder,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val getSyncUploadsFolderIdsUseCase: GetSyncUploadsFolderIdsUseCase,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
) : ViewModel() {
    private val triggerFlow = MutableStateFlow(false)
    private val queryFlow = MutableStateFlow<String?>(null)
    private val locationFilterOptionFlow = MutableStateFlow(LocationFilterOption.AllLocations)
    private val durationFilterOptionFlow = MutableStateFlow(DurationFilterOption.AllDurations)
    private val selectedVideoIdsFlow = MutableStateFlow<List<Long>>(emptyList())
    private val showHiddenItemsFlow = observeShowHiddenItemsState().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        false
    )

    internal val uiState: StateFlow<VideosTabUiState> by lazy {
        triggerFlow.flatMapLatest {
            flow {
                emit(VideosTabUiState.Loading)
                emitAll(
                    merge(
                        monitorNodeUpdatesUseCase().filter {
                            it.changes.keys.any { node ->
                                node is FileNode && node.type is VideoFileTypeInfo
                            }
                        },
                        monitorOfflineNodeUpdatesUseCase(),
                        monitorSortCloudOrderUseCase(),
                        selectedVideoIdsFlow,
                        showHiddenItemsFlow,
                    ).mapLatest {
                        val videoNodes = getVideoNodeList()
                            .filterVideosByDuration()
                            .filterVideosByLocation()
                            .filterNonSensitiveItems(showHiddenItemsFlow.value)
                        val uiEntities = videoNodes.map {
                            videoUiEntityMapper(it).copy(
                                isSelected = it.id.longValue in selectedVideoIdsFlow.value
                            )
                        }
                        val sortOrder = getCloudSortOrder()
                        val sortOrderPair = nodeSortConfigurationUiMapper(sortOrder)
                        val selectedNodes = videoNodes.filter {
                            it.id.longValue in selectedVideoIdsFlow.value
                        }

                        VideosTabUiState.Data(
                            allVideoEntities = uiEntities,
                            allVideoNodes = videoNodes,
                            sortOrder = sortOrder,
                            query = queryFlow.value,
                            selectedSortConfiguration = sortOrderPair,
                            locationSelectedFilterOption = locationFilterOptionFlow.value,
                            durationSelectedFilterOption = durationFilterOptionFlow.value,
                            selectedTypedNodes = selectedNodes,
                            showHiddenItems = showHiddenItemsFlow.value
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


    private suspend fun getVideoNodeList() =
        getAllVideosUseCase(
            searchQuery = getCurrentSearchQuery(),
            tag = getCurrentSearchQuery().removePrefix("#"),
            description = getCurrentSearchQuery()
        )

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

    private fun observeShowHiddenItemsState() =
        combine(
            monitorHiddenNodesEnabledUseCase()
                .catch { Timber.e(it) },
            monitorShowHiddenItemsUseCase()
                .catch { Timber.e(it) },
        ) { isHiddenNodesEnabled, showHiddenItems ->
            showHiddenItems || !isHiddenNodesEnabled
        }

    internal fun onItemClicked(item: VideoUiEntity) {
        if (selectedVideoIdsFlow.value.isNotEmpty()) {
            toggleItemSelection(item = item)
        }
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
        if (uiState.value is VideosTabUiState.Data) {
            val allIds =
                (uiState.value as VideosTabUiState.Data).allVideoEntities.map { it.id.longValue }
            selectedVideoIdsFlow.update { allIds }
        }
    }

    internal fun clearSelection() {
        selectedVideoIdsFlow.update { emptyList() }
    }
}