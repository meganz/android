package mega.privacy.android.feature.cloudexplorer.presentation.favouritesexplorer

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.favourites.GetAllFavoritesUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerSharedViewModel
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.shared.nodes.mapper.NodeUiItemMapper
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FavouritesExplorerViewModel @Inject constructor(
    monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    monitorViewTypeUseCase: MonitorViewType,
    setViewTypeUseCase: SetViewType,
    monitorStorageStateUseCase: MonitorStorageStateUseCase,
    monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
    setCloudSortOrderUseCase: SetCloudSortOrder,
    nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    nodeUiItemMapper: NodeUiItemMapper,
    private val getAllFavoritesUseCase: GetAllFavoritesUseCase,
) : NodeExplorerSharedViewModel(
    monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
    monitorViewTypeUseCase = monitorViewTypeUseCase,
    setViewTypeUseCase = setViewTypeUseCase,
    monitorStorageStateUseCase = monitorStorageStateUseCase,
    monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
    monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
    monitorSortCloudOrderUseCase = monitorSortCloudOrderUseCase,
    setCloudSortOrderUseCase = setCloudSortOrderUseCase,
    nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
    nodeUiItemMapper = nodeUiItemMapper,
    args = Args(
        nodeId = NodeId(-1),
        nodeSourceType = NodeSourceType.FAVOURITES,
    ),
) {

    private val _favouritesExplorerInternalUiState = MutableStateFlow(FavouritesExplorerUiState())
    val favouritesExplorerUiState = _favouritesExplorerInternalUiState.asStateFlow()

    override fun loadNodes() {
        viewModelScope.launch {
            setItems(
                nodes = getAllFavoritesUseCase().first().filterIsInstance<TypedFolderNode>(),
                nodesLoadingState = NodesLoadingState.FullyLoaded,
            )
        }
    }

    override fun refreshNodes() {
        loadNodes()
    }

    override fun monitorNodeUpdates() {
        viewModelScope.launch {
            getAllFavoritesUseCase()
                .catch { Timber.e(it) }
                .collectLatest { nodes ->
                    setItems(
                        nodes = nodes.filterIsInstance<TypedFolderNode>(),
                        nodesLoadingState = NodesLoadingState.FullyLoaded,
                    )
                }
        }
    }
}