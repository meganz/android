package mega.privacy.android.feature.cloudexplorer.presentation.nodeexplorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.mobile.analytics.event.ViewModeGridMenuItemEvent
import mega.privacy.mobile.analytics.event.ViewModeListMenuItemEvent
import timber.log.Timber


abstract class NodeExplorerViewModel(
    private val monitorViewTypeUseCase: MonitorViewType,
    private val setViewTypeUseCase: SetViewType,
    private val monitorStorageStateUseCase: MonitorStorageStateUseCase,
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    private val args: Args,
) : ViewModel() {
    private val _nodeExplorerUiState = MutableStateFlow(NodeExplorerUiState())
    val nodeExplorerUiState = _nodeExplorerUiState.asStateFlow()

    init {
        monitorViewType()
        monitorSortOrder()
        monitorStorageOverQuota()
        monitorNodeUpdates()
    }

    private fun monitorViewType() {
        viewModelScope.launch {
            monitorViewTypeUseCase()
                .catch { Timber.e(it) }
                .collect { viewType ->
                    _nodeExplorerUiState.update { it.copy(viewType = viewType) }
                }
        }
    }

    fun updateViewType() {
        viewModelScope.launch {
            runCatching {
                val toggledViewType = when (_nodeExplorerUiState.value.viewType) {
                    ViewType.LIST -> ViewType.GRID
                    ViewType.GRID -> ViewType.LIST
                }
                setViewTypeUseCase(toggledViewType)
                toggledViewType
            }.onFailure {
                Timber.e(it, "Failed to change view type")
            }.onSuccess { viewType ->
                val event = when (viewType) {
                    ViewType.LIST -> ViewModeListMenuItemEvent
                    ViewType.GRID -> ViewModeGridMenuItemEvent
                }
                Analytics.tracker.trackEvent(event)
            }
        }
    }

    private fun monitorStorageOverQuota() {
        viewModelScope.launch {
            monitorStorageStateUseCase().collectLatest { storageState ->
                val isStorageOverQuota = storageState == StorageState.Red
                        || storageState == StorageState.PayWall
                _nodeExplorerUiState.update { state ->
                    state.copy(isStorageOverQuota = isStorageOverQuota)
                }
            }
        }
    }

    private fun monitorNodeUpdates() {
        viewModelScope.launch {
            monitorNodeUpdatesByIdUseCase(
                nodeId = args.nodeId,
                nodeSourceType = args.nodeSourceType,
            ).catch { Timber.e(it) }
                .collectLatest { change ->
                    if (change == NodeChanges.Remove) {
                        // If current folder is moved to rubbish bin, navigate back
                        _nodeExplorerUiState.update { state -> state.copy(navigateBack = triggered) }
                    } else {
                        loadNodes()
                    }
                }
        }
    }

    fun updateSortOrder(
        nodeSortConfiguration: NodeSortConfiguration,
        sortOrder: SortOrder,
    ) {
        _nodeExplorerUiState.update {
            it.copy(
                nodeSortConfiguration = nodeSortConfiguration,
                sortOrder = sortOrder
            )
        }
    }

    fun setItems(items: List<NodeUiItem<TypedNode>>) {
        _nodeExplorerUiState.update { state -> state.copy(items = items) }
    }

    fun onNavigateBackEventConsumed() {
        _nodeExplorerUiState.update { state ->
            state.copy(navigateBack = consumed)
        }
    }

    abstract fun loadNodes()
    abstract fun monitorSortOrder()
    abstract fun updateNodeSortConfiguration(nodeSortConfiguration: NodeSortConfiguration)

    data class Args(
        val nodeId: NodeId,
        val nodeSourceType: NodeSourceType,
    )
}