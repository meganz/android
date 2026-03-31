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
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.usecase.GetNodeInfoByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.shared.nodes.R as NodesR
import mega.privacy.android.shared.nodes.mapper.NodeViewItemMapper
import timber.log.Timber

@HiltViewModel(assistedFactory = NodesExplorerViewModel.Factory::class)
class NodesExplorerViewModel @AssistedInject constructor(
    monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    monitorStorageStateUseCase: MonitorStorageStateUseCase,
    monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    nodeViewItemMapper: NodeViewItemMapper,
    private val getFileBrowserNodeChildrenUseCase: GetFileBrowserNodeChildrenUseCase,
    private val getNodesByIdInChunkUseCase: GetNodesByIdInChunkUseCase,
    private val getNodeInfoByIdUseCase: GetNodeInfoByIdUseCase,
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
    @Assisted private val args: Args,
) : NodeExplorerSharedViewModel(
    monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
    monitorStorageStateUseCase = monitorStorageStateUseCase,
    monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
    monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
    nodeViewItemMapper = nodeViewItemMapper,
    args = args,
) {

    private val _nodesExplorerInternalUiState = MutableStateFlow(NodesExplorerUiState())
    val nodesExplorerUiState = _nodesExplorerInternalUiState.asStateFlow()

    init {
        monitorNodeUpdates()
        checkRootNode()
        updateFolderName()
    }

    override fun loadNodes() {
        viewModelScope.launch {
            getNodesByIdInChunkUseCase(args.nodeId)
                .catch { Timber.e(it) }
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

    private fun checkRootNode() {
        viewModelScope.launch {
            runCatching { getRootNodeIdUseCase() }
                .onFailure { Timber.e(it) }
                .getOrDefault(NodeId(-1))?.let { id ->
                    _nodesExplorerInternalUiState.update { state ->
                        state.copy(isRoot = id == args.nodeId)
                    }
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