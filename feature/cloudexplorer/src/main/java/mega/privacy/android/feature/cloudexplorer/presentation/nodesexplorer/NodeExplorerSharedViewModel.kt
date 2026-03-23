package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.shared.nodes.mapper.NodeUiItemMapper
import mega.privacy.android.shared.nodes.model.NodeUiItem
import timber.log.Timber

/**
 * Shared ViewModel for node explorer screens (cloud, incoming shares, favourites).
 */
abstract class NodeExplorerSharedViewModel(
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    private val monitorStorageStateUseCase: MonitorStorageStateUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val nodeUiItemMapper: NodeUiItemMapper,
    private val args: Args,
) : ViewModel() {

    private val _nodedExplorerSharedUiState = MutableStateFlow(NodesExplorerSharedUiState())
    val nodeExplorerSharedUiState = _nodedExplorerSharedUiState.asStateFlow()

    init {
        _nodedExplorerSharedUiState.update { state ->
            state.copy(
                currentFolderId = args.nodeId,
                nodeSourceType = args.nodeSourceType
            )
        }
        monitorHiddenNodes()
        monitorStorageOverQuota()
    }

    fun monitorNodeUpdates() {
        viewModelScope.launch {
            monitorNodeUpdatesByIdUseCase(
                nodeId = args.nodeId,
                nodeSourceType = args.nodeSourceType,
            ).onStart {
                loadNodes()
            }.catch { Timber.e(it) }
                .collectLatest { change ->
                    if (change == NodeChanges.Remove) {
                        _nodedExplorerSharedUiState.update { state -> state.copy(navigateBack = triggered) }
                    } else {
                        refreshNodes()
                    }
                }
        }
    }

    private fun monitorStorageOverQuota() {
        viewModelScope.launch {
            monitorStorageStateUseCase().collectLatest { storageState ->
                val isStorageOverQuota = storageState == StorageState.Red
                        || storageState == StorageState.PayWall
                _nodedExplorerSharedUiState.update { state ->
                    state.copy(isStorageOverQuota = isStorageOverQuota)
                }
            }
        }
    }

    private fun monitorHiddenNodes() {
        viewModelScope.launch {
            combine(
                monitorHiddenNodesEnabledUseCase()
                    .catch { Timber.e(it) },
                monitorShowHiddenItemsUseCase()
                    .catch { Timber.e(it) },
            ) { isHiddenNodesEnabled, showHiddenItems ->
                _nodedExplorerSharedUiState.update { state ->
                    state.copy(
                        isHiddenNodeSettingsLoading = false,
                        isHiddenNodesEnabled = isHiddenNodesEnabled,
                        showHiddenNodes = showHiddenItems
                    )
                }
            }.collect()
        }
    }

    fun setItems(nodes: List<TypedNode>, nodesLoadingState: NodesLoadingState) {
        viewModelScope.launch {
            val nodeUiItems = nodeUiItemMapper(
                nodeList = nodes,
                nodeSourceType = args.nodeSourceType,
                existingItems = nodeExplorerSharedUiState.value.items,
            )

            _nodedExplorerSharedUiState.update { state ->
                state.copy(
                    items = nodeUiItems,
                    nodesLoadingState = nodesLoadingState,
                )
            }
        }
    }

    fun onNavigateBackEventConsumed() {
        _nodedExplorerSharedUiState.update { state ->
            state.copy(navigateBack = consumed)
        }
    }

    fun fileClicked(item: NodeUiItem<TypedNode>) {

    }

    abstract fun loadNodes()
    abstract fun refreshNodes()

    data class Args(
        val nodeId: NodeId,
        val nodeSourceType: NodeSourceType,
    )
}
