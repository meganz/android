package mega.privacy.android.feature.cloudexplorer.presentation.incomingsharesexplorer

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.SetOthersSortOrder
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.shares.GetIncomingSharesChildrenNodeUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerSharedViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class IncomingSharesExplorerViewModel @Inject constructor(
    monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    monitorViewTypeUseCase: MonitorViewType,
    setViewTypeUseCase: SetViewType,
    monitorStorageStateUseCase: MonitorStorageStateUseCase,
    monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
    nodeUiItemMapper: NodeUiItemMapper,
    setCloudSortOrderUseCase: SetCloudSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val getIncomingSharesChildrenNodeUseCase: GetIncomingSharesChildrenNodeUseCase,
    private val getOthersSortOrder: GetOthersSortOrder,
    private val setOthersSortOrder: SetOthersSortOrder,
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
        nodeSourceType = NodeSourceType.INCOMING_SHARES,
    ),
) {

    private val _incomingSharesExplorerInternalUiState =
        MutableStateFlow(IncomingSharesExplorerUiState())
    val incomingSharesExplorerUiState = _incomingSharesExplorerInternalUiState.asStateFlow()

    override fun loadNodes() {
        viewModelScope.launch {
            setItems(
                nodes = getIncomingSharesChildrenNodeUseCase(args.nodeId.longValue),
                nodesLoadingState = NodesLoadingState.FullyLoaded
            )
        }
    }

    override fun refreshNodes() {
        loadNodes()
    }

    override fun monitorSortOrder() {
        getSortOrder()
    }

    private fun getSortOrder(
        refresh: Boolean = false,
    ) {
        viewModelScope.launch {
            runCatching {
                getOthersSortOrder()
            }.onSuccess { sortOrder ->
                updateSortOrder(nodeSortConfigurationUiMapper(sortOrder), sortOrder)

                if (refresh) {
                    refreshNodes()
                }
            }.onFailure {
                Timber.e(it, "Failed to get sort order")
            }
        }
    }

    override fun updateNodeSortConfiguration(nodeSortConfiguration: NodeSortConfiguration) {
        viewModelScope.launch {
            runCatching {
                val order = nodeSortConfigurationUiMapper(nodeSortConfiguration)
                setOthersSortOrder(order)
            }.onSuccess {
                getSortOrder(refresh = true)
            }.onFailure {
                Timber.Forest.e(it, "Failed to set others sort order")
            }
        }
    }
}