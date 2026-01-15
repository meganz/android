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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeSourceTypeToViewTypeMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.node.GetNodeContentUriByHandleUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.videosection.GetAllVideosUseCase
import mega.privacy.android.domain.usecase.videosection.GetSyncUploadsFolderIdsUseCase
import mega.privacy.android.feature.photos.mapper.VideoUiEntityMapper
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import mega.privacy.android.navigation.destination.LegacyMediaPlayerNavKey
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VideosTabViewModel @Inject constructor(
    private val getAllVideosUseCase: GetAllVideosUseCase,
    private val videoUiEntityMapper: VideoUiEntityMapper,
    private val getSyncUploadsFolderIdsUseCase: GetSyncUploadsFolderIdsUseCase,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
    private val nodeSourceTypeToViewTypeMapper: NodeSourceTypeToViewTypeMapper,
    private val getNodeContentUriByHandleUseCase: GetNodeContentUriByHandleUseCase,
) : ViewModel() {
    private val queryFlow = MutableStateFlow<String?>(null)
    private val selectedVideoIdsFlow = MutableStateFlow<List<Long>>(emptyList())

    private val navigateToVideoPlayerFlow =
        MutableStateFlow<StateEventWithContent<NavKey>>(consumed())
    internal val navigateToVideoPlayerEvent: StateFlow<StateEventWithContent<NavKey>>
            by lazy(LazyThreadSafetyMode.NONE) {
                navigateToVideoPlayerFlow
            }

    private val sortOrder: StateFlow<SortOrder> by lazy {
        monitorSortCloudOrderUseCase()
            .mapLatest { it ?: SortOrder.ORDER_DEFAULT_ASC }
            .catch { Timber.e(it) }
            .asUiStateFlow(
                viewModelScope,
                SortOrder.ORDER_DEFAULT_ASC
            )
    }

    private fun combinedTriggerFlow(): Flow<Unit> = merge(
        monitorNodeUpdatesUseCase().filter {
            it.changes.keys.any { node ->
                node is FileNode && node.type is VideoFileTypeInfo
            }
        }.mapLatest { }
            .catch { Timber.e(it) },
        monitorOfflineNodeUpdatesUseCase().mapLatest { }
            .catch { Timber.e(it) },
        monitorSortCloudOrderUseCase().mapLatest { }
            .catch { Timber.d(it) }
    ).onStart { emit(Unit) }

    private fun getVideoListFlow(): Flow<Pair<List<TypedVideoNode>, String?>> =
        combinedTriggerFlow().flatMapLatest {
            queryFlow.mapLatest { query ->
                getAllVideosUseCase(
                    searchQuery = query.orEmpty(),
                    tag = query?.removePrefix("#"),
                    description = query
                ) to query
            }
        }

    private fun getShowHiddenItemsFlow(): Flow<Boolean> = combine(
        monitorHiddenNodesEnabledUseCase().catch { Timber.e(it) },
        monitorShowHiddenItemsUseCase().catch { Timber.e(it) },
    ) { isHiddenNodesEnabled, isShowHiddenItems ->
        isShowHiddenItems || !isHiddenNodesEnabled
    }

    internal val uiState: StateFlow<VideosTabUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            getVideoListFlow(),
            sortOrder,
            getShowHiddenItemsFlow(),
            flow { emit(getSyncUploadsFolderIdsUseCase()) },
            selectedVideoIdsFlow,
        ) { (videoList, query), sortOrder, showHiddenItems, syncUploadsFolderIds, selectedItems ->
            val videoNodes = videoList
                .filterNonSensitiveItems(showHiddenItems)
            val uiEntities = videoNodes.map { videoUiEntityMapper(it, syncUploadsFolderIds) }
                .map { it.copy(isSelected = it.id.longValue in selectedItems) }
            val sortOrderPair = nodeSortConfigurationUiMapper(sortOrder)

            VideosTabUiState.Data(
                allVideoEntities = uiEntities,
                query = query,
                selectedSortConfiguration = sortOrderPair,
                showHiddenItems = showHiddenItems,
                selectedTypedNodes = videoNodes.filter { it.id.longValue in selectedItems }
            )
        }.asUiStateFlow(
            viewModelScope,
            VideosTabUiState.Loading
        )
    }

    private fun List<TypedVideoNode>.filterNonSensitiveItems(showHiddenItems: Boolean) =
        if (showHiddenItems) {
            this
        } else {
            filterNot { it.isMarkedSensitive || it.isSensitiveInherited }
        }

    internal fun getCurrentSearchQuery() = queryFlow.value.orEmpty()

    internal fun searchQuery(queryString: String?) {
        if (queryFlow.value != queryString) {
            queryFlow.update { queryString }
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
            val uri = runCatching {
                getNodeContentUriByHandleUseCase(item.id.longValue)
            }.onFailure { Timber.e(it) }.getOrNull() ?: return@launch

            val content = FileNodeContent.AudioOrVideo(uri = uri)

            val isSearchMode = queryFlow.value.isNullOrEmpty()

            val navKey = LegacyMediaPlayerNavKey(
                nodeHandle = item.id.longValue,
                nodeContentUri = content.uri,
                nodeSourceType = nodeSourceTypeToViewTypeMapper(
                    if (isSearchMode) {
                        NodeSourceType.SEARCH
                    } else {
                        NodeSourceType.VIDEOS
                    }
                ),
                sortOrder = sortOrder.value,
                isFolderLink = false,
                fileName = item.name,
                parentHandle = item.parentId.longValue,
                fileHandle = item.id.longValue,
                fileTypeInfo = item.fileTypeInfo,
                searchedItems = if (isSearchMode) {
                    (uiState.value as? VideosTabUiState.Data)?.allVideoEntities?.map { it.id.longValue }
                } else {
                    null
                },
                nodeHandles = null,
            )

            navigateToVideoPlayerFlow.update { triggered(navKey) }

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