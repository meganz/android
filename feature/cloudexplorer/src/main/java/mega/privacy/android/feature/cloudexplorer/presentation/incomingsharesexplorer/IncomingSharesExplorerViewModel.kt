package mega.privacy.android.feature.cloudexplorer.presentation.incomingsharesexplorer

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.node.GetNodeNavigationStackUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.search.SearchUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.shares.GetIncomingSharesChildrenNodeUseCase
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerSharedViewModel
import mega.privacy.android.shared.nodes.mapper.NodeSourceTypeToSearchTargetMapper
import mega.privacy.android.shared.nodes.mapper.NodeViewItemMapper
import javax.inject.Inject

@HiltViewModel
class IncomingSharesExplorerViewModel @Inject constructor(
    monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    monitorStorageStateUseCase: MonitorStorageStateUseCase,
    monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    nodeViewItemMapper: NodeViewItemMapper,
    getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    searchUseCase: SearchUseCase,
    nodeSourceTypeToSearchTargetMapper: NodeSourceTypeToSearchTargetMapper,
    getNodeNavigationStackUseCase: GetNodeNavigationStackUseCase,
    private val getIncomingSharesChildrenNodeUseCase: GetIncomingSharesChildrenNodeUseCase,
) : NodeExplorerSharedViewModel(
    monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
    monitorStorageStateUseCase = monitorStorageStateUseCase,
    monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
    monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
    nodeViewItemMapper = nodeViewItemMapper,
    getContactVerificationWarningUseCase = getContactVerificationWarningUseCase,
    searchUseCase = searchUseCase,
    nodeSourceTypeToSearchTargetMapper = nodeSourceTypeToSearchTargetMapper,
    getNodeNavigationStackUseCase = getNodeNavigationStackUseCase,
    args = Args(
        nodeId = NodeId(-1),
        nodeSourceType = NodeSourceType.INCOMING_SHARES,
    ),
) {

    private val _incomingSharesExplorerInternalUiState =
        MutableStateFlow(IncomingSharesExplorerUiState())
    val incomingSharesExplorerUiState = _incomingSharesExplorerInternalUiState.asStateFlow()

    init {
        monitorNodeUpdates()
    }

    override fun loadNodes() {
        viewModelScope.launch {
            setItems(
                nodes = getIncomingSharesChildrenNodeUseCase(-1),
                nodesLoadingState = NodesLoadingState.FullyLoaded
            )
        }
    }

    override fun refreshNodes() {
        loadNodes()
    }
}
