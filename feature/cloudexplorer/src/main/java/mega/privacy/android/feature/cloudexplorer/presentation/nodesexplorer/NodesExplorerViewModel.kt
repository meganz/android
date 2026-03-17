package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.usecase.GetNodeInfoByIdUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import timber.log.Timber
import mega.privacy.android.shared.nodes.R as NodesR

@HiltViewModel(assistedFactory = NodesExplorerViewModel.Factory::class)
class NodesExplorerViewModel @AssistedInject constructor(
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
    private val getFileBrowserNodeChildrenUseCase: GetFileBrowserNodeChildrenUseCase,
    private val getNodesByIdInChunkUseCase: GetNodesByIdInChunkUseCase,
    private val getNodeInfoByIdUseCase: GetNodeInfoByIdUseCase,
    @Assisted override val args: Args,
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
    args = args,
) {

    private val _nodesExplorerInternalUiState = MutableStateFlow(NodesExplorerUiState())
    val nodesExplorerUiState = _nodesExplorerInternalUiState.asStateFlow()

    init {
        updateFolderName()
    }

    override fun loadNodes() {
        viewModelScope.launch {
            getNodesByIdInChunkUseCase(args.nodeId)
                .catch { Timber.Forest.e(it) }
                .collect { (nodes, hasMore) ->
                    updateFolderName()
                    setItems(
                        nodes = nodes,
                        nodesLoadingState = if (hasMore) {
                            NodesLoadingState.PartiallyLoaded
                        } else {
                            NodesLoadingState.FullyLoaded
                        },
                    )
                }
        }
    }

    override fun refreshNodes() {
        viewModelScope.launch {
            runCatching {
                setItems(
                    nodes = getFileBrowserNodeChildrenUseCase(args.nodeId.longValue),
                    nodesLoadingState = NodesLoadingState.FullyLoaded
                )
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun updateFolderName() {
        viewModelScope.launch {
            runCatching {
                getNodeInfoByIdUseCase(args.nodeId)
            }.onSuccess { nodeInfo ->
                val folderName = if (nodeInfo?.isNodeKeyDecrypted == false) {
                    LocalizedText.StringRes(resId = NodesR.string.shared_items_verify_credentials_undecrypted_folder)
                } else {
                    LocalizedText.Literal(nodeInfo?.name ?: "")
                }
                // Only update state if fetched title is different
                if (nodesExplorerUiState.value.folderName != folderName) {
                    _nodesExplorerInternalUiState.update { state -> state.copy(folderName = folderName) }
                }
            }.onFailure {
                Timber.e(it, "Failed to get node name for title update")
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(args: Args): NodesExplorerViewModel
    }
}