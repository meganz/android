package mega.privacy.android.feature.cloudexplorer.presentation.clouddriveexplorer

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
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
import mega.privacy.android.feature.cloudexplorer.presentation.nodeexplorer.NodeExplorerViewModel
import timber.log.Timber

@HiltViewModel(assistedFactory = CloudDriveExplorerViewModel.Factory::class)
class CloudDriveExplorerViewModel @AssistedInject constructor(
    monitorViewTypeUseCase: MonitorViewType,
    setViewTypeUseCase: SetViewType,
    monitorStorageStateUseCase: MonitorStorageStateUseCase,
    monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    private val getFileBrowserNodeChildrenUseCase: GetFileBrowserNodeChildrenUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val getNodesByIdInChunkUseCase: GetNodesByIdInChunkUseCase,
    private val getNodeInfoByIdUseCase: GetNodeInfoByIdUseCase,
    private val nodeUiItemMapper: NodeUiItemMapper,
    @Assisted private val args: Args,
) : NodeExplorerViewModel(
    monitorViewTypeUseCase = monitorViewTypeUseCase,
    setViewTypeUseCase = setViewTypeUseCase,
    monitorStorageStateUseCase = monitorStorageStateUseCase,
    monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
    args = args,
) {

    private val _cloudDriveExplorerInternalUiState =
        MutableStateFlow(CloudDriveExplorerInternalUiState())

    val cloudDriveExplorerUiState = combine(
        nodeExplorerUiState,
        _cloudDriveExplorerInternalUiState.asStateFlow(),
    ) { nodeExplorerUiState, cloudExplorerInternalUIState ->
        CloudDriveExplorerUiState(
            cloudDriveExplorerInternalUIState = cloudExplorerInternalUIState,
            nodeExplorerUiState = nodeExplorerUiState,
        )
    } as StateFlow<CloudDriveExplorerUiState>

    init {
        updateFolderName()
        monitorHiddenNodes()
    }

    private fun monitorHiddenNodes() {
        viewModelScope.launch {
            combine(
                monitorHiddenNodesEnabledUseCase()
                    .catch { Timber.e(it) },
                monitorShowHiddenItemsUseCase()
                    .catch { Timber.e(it) },
            ) { isHiddenNodesEnabled, showHiddenItems ->
                _cloudDriveExplorerInternalUiState.update { state ->
                    state.copy(
                        isHiddenNodeSettingsLoading = false,
                        isHiddenNodesEnabled = isHiddenNodesEnabled,
                        showHiddenNodes = showHiddenItems
                    )
                }
            }.collect()
        }
    }

    private fun updateFolderName() {
        viewModelScope.launch {
            runCatching {
                getNodeInfoByIdUseCase(args.nodeId)
            }.onSuccess { nodeInfo ->
                val folderName = if (nodeInfo?.isNodeKeyDecrypted == false) {
                    LocalizedText.StringRes(resId = R.string.shared_items_verify_credentials_undecrypted_folder)
                } else {
                    LocalizedText.Literal(nodeInfo?.name ?: "")
                }
                // Only update state if fetched title is different
                if (cloudDriveExplorerUiState.value.folderName != folderName) {
                    _cloudDriveExplorerInternalUiState.update { state -> state.copy(folderName = folderName) }
                }
            }.onFailure {
                Timber.e(it, "Failed to get node name for title update")
            }
        }
    }

    override fun monitorSortOrder() {
        monitorSortCloudOrderUseCase()
            .catch { Timber.e(it) }
            .filterNotNull()
            .onEach { sortOrder ->
                updateSortOrder(nodeSortConfigurationUiMapper(sortOrder), sortOrder)
                refreshNodes()
            }
            .launchIn(viewModelScope)
    }

    override fun updateNodeSortConfiguration(nodeSortConfiguration: NodeSortConfiguration) {
        viewModelScope.launch {
            runCatching {
                val order = nodeSortConfigurationUiMapper(nodeSortConfiguration)
                setCloudSortOrderUseCase(order)
            }.onFailure {
                Timber.e(it, "Failed to set cloud sort order")
            }
        }
    }

    private fun refreshNodes() {
        viewModelScope.launch {
            runCatching {
                setNodes(getFileBrowserNodeChildrenUseCase(args.nodeId.longValue))
                _cloudDriveExplorerInternalUiState.update { state ->
                    state.copy(nodesLoadingState = NodesLoadingState.FullyLoaded)
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private suspend fun setNodes(nodes: List<TypedNode>) {
        val nodeUiItems = nodeUiItemMapper(
            nodeList = nodes,
            nodeSourceType = args.nodeSourceType,
            existingItems = cloudDriveExplorerUiState.value.items,
        )

        setItems(nodeUiItems)
    }

    override fun loadNodes() {
        viewModelScope.launch {
            getNodesByIdInChunkUseCase(args.nodeId)
                .catch { Timber.e(it) }
                .collect { (nodes, hasMore) ->
                    updateFolderName()
                    setNodes(nodes)

                    _cloudDriveExplorerInternalUiState.update { state ->
                        state.copy(
                            nodesLoadingState = if (hasMore) {
                                NodesLoadingState.PartiallyLoaded
                            } else {
                                NodesLoadingState.FullyLoaded
                            },
                        )
                    }
                }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(args: Args): CloudDriveExplorerViewModel
    }
}