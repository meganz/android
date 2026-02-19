package mega.privacy.android.feature.photos.presentation.playlists.videoselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.mapper.SelectVideoItemUiEntityMapper
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.model.SelectVideoItemUiEntity
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = SelectVideosForPlaylistViewModel.Factory::class)
class SelectVideosForPlaylistViewModel @AssistedInject constructor(
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
    private val getNodesByIdInChunkUseCase: GetNodesByIdInChunkUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
    private val selectVideoItemUiEntityMapper: SelectVideoItemUiEntityMapper,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val setViewTypeUseCase: SetViewType,
    private val monitorViewTypeUseCase: MonitorViewType,
    @Assisted private val nodeHandle: Long,
    @Assisted private val nodeName: String?,
) : ViewModel() {
    private val queryFlow = MutableStateFlow<String?>(null)

    internal val navigateToFolderEvent: StateFlow<StateEventWithContent<SelectVideoItemUiEntity>>
        field: MutableStateFlow<StateEventWithContent<SelectVideoItemUiEntity>> = MutableStateFlow(
            consumed()
        )

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
        monitorSortCloudOrderUseCase().mapLatest { }
            .catch { Timber.d(it) }
    ).onStart { emit(Unit) }

    private fun getNodesFlow(): Flow<Pair<List<TypedNode>, Boolean>> =
        combinedTriggerFlow().flatMapLatest {
            val folderId = NodeId(nodeHandle)
            val folderOrRootNodeId = if (folderId.longValue == -1L) {
                getRootNodeIdUseCase() ?: folderId
            } else {
                folderId
            }
            getNodesByIdInChunkUseCase(folderOrRootNodeId).map { (nodes, hasMore) ->
                val items = nodes.filter {
                    it is TypedFolderNode || (it is TypedFileNode && it.type is VideoFileTypeInfo)
                }
                items to hasMore
            }
        }

    private fun getShowHiddenItemsFlow(): Flow<Boolean> = combine(
        monitorHiddenNodesEnabledUseCase().catch { Timber.e(it) },
        monitorShowHiddenItemsUseCase().catch { Timber.e(it) },
    ) { isHiddenNodesEnabled, isShowHiddenItems ->
        isShowHiddenItems || !isHiddenNodesEnabled
    }

    internal val uiState: StateFlow<SelectVideosForPlaylistUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            getNodesFlow().catch { Timber.d(it) },
            sortOrder,
            queryFlow,
            getShowHiddenItemsFlow(),
            monitorViewTypeUseCase().catch { Timber.e(it) },
        ) { (nodes, hasMore), sortOrder, query, showHiddenItems, viewType ->
            val items =
                nodes.filterNonSensitiveItems(showHiddenItems).map {
                    selectVideoItemUiEntityMapper(it)
                }.queryItems(query ?: "")
            val sortOrderPair = nodeSortConfigurationUiMapper(sortOrder)
            SelectVideosForPlaylistUiState.Data(
                title = LocalizedText.Literal(nodeName ?: ""),
                isCloudDriveRoot = nodeHandle == -1L,
                items = items,
                showHiddenItems = showHiddenItems,
                selectedSortConfiguration = sortOrderPair,
                query = query,
                currentViewType = viewType,
                nodesLoadingState = if (hasMore) {
                    NodesLoadingState.PartiallyLoaded
                } else {
                    NodesLoadingState.FullyLoaded
                }
            )
        }.asUiStateFlow(
            viewModelScope,
            SelectVideosForPlaylistUiState.Loading
        )
    }

    private fun List<TypedNode>.filterNonSensitiveItems(showHiddenItems: Boolean) =
        if (showHiddenItems) {
            this
        } else {
            filterNot { it.isMarkedSensitive || it.isSensitiveInherited }
        }

    private fun List<SelectVideoItemUiEntity>.queryItems(query: String) =
        filter {
            it.name.contains(query, ignoreCase = true)
        }

    internal fun itemClicked(item: SelectVideoItemUiEntity) {
        if (item.isFolder) {
            navigateToFolderEvent.update { triggered(item) }
        } else {
            //TODO click file item
        }
    }

    internal fun resetNavigateToFolderEvent() {
        navigateToFolderEvent.update { consumed() }
    }

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

    /**
     * This method will toggle node view type between list and grid.
     */
    internal fun changeViewTypeClicked() {
        viewModelScope.launch {
            runCatching {
                val currentViewType =
                    (uiState.value as? SelectVideosForPlaylistUiState.Data)?.currentViewType
                        ?: ViewType.LIST
                val toggledViewType = when (currentViewType) {
                    ViewType.LIST -> ViewType.GRID
                    ViewType.GRID -> ViewType.LIST
                }
                setViewTypeUseCase(toggledViewType)
            }.onFailure {
                Timber.e(it, "Failed to change view type")
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(nodeHandle: Long, nodeName: String?): SelectVideosForPlaylistViewModel
    }
}