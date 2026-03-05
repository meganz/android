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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.mobile.analytics.event.ViewModeGridMenuItemEvent
import mega.privacy.mobile.analytics.event.ViewModeListMenuItemEvent
import timber.log.Timber

abstract class NodeExplorerSharedViewModel(
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    private val monitorViewTypeUseCase: MonitorViewType,
    private val setViewTypeUseCase: SetViewType,
    private val monitorStorageStateUseCase: MonitorStorageStateUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val nodeUiItemMapper: NodeUiItemMapper,
    open val args: Args,
) : ViewModel() {
    private val _nodedExplorerSharedUiState = MutableStateFlow(NodesExplorerSharedUiState())
    val nodeExplorerSharedUiState = _nodedExplorerSharedUiState.asStateFlow()

    init {
        monitorNodeUpdates()
        monitorHiddenNodes()
        monitorViewType()
        monitorSortOrder()
        monitorStorageOverQuota()
    }

    open fun monitorNodeUpdates() {
        viewModelScope.launch {
            monitorNodeUpdatesByIdUseCase(
                nodeId = args.nodeId,
                nodeSourceType = args.nodeSourceType,
            ).catch { Timber.Forest.e(it) }
                .collectLatest { change ->
                    if (change == NodeChanges.Remove) {
                        // If current folder is moved to rubbish bin, navigate back
                        _nodedExplorerSharedUiState.update { state -> state.copy(navigateBack = triggered) }
                    } else {
                        refreshNodes()
                    }
                }
        }
    }

    private fun monitorViewType() {
        viewModelScope.launch {
            monitorViewTypeUseCase()
                .catch { Timber.Forest.e(it) }
                .collect { viewType ->
                    _nodedExplorerSharedUiState.update { it.copy(viewType = viewType) }
                }
        }
    }

    fun updateViewType() {
        viewModelScope.launch {
            runCatching {
                val toggledViewType = when (_nodedExplorerSharedUiState.value.viewType) {
                    ViewType.LIST -> ViewType.GRID
                    ViewType.GRID -> ViewType.LIST
                }
                setViewTypeUseCase(toggledViewType)
                toggledViewType
            }.onFailure {
                Timber.Forest.e(it, "Failed to change view type")
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
                    .catch { Timber.Forest.e(it) },
                monitorShowHiddenItemsUseCase()
                    .catch { Timber.Forest.e(it) },
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

    open fun monitorSortOrder() {
        monitorSortCloudOrderUseCase()
            .catch { Timber.Forest.e(it) }
            .filterNotNull()
            .onEach { sortOrder ->
                updateSortOrder(nodeSortConfigurationUiMapper(sortOrder), sortOrder)
                refreshNodes()
            }
            .launchIn(viewModelScope)
    }

    fun updateSortOrder(
        nodeSortConfiguration: NodeSortConfiguration,
        sortOrder: SortOrder,
    ) {
        _nodedExplorerSharedUiState.update {
            it.copy(
                nodeSortConfiguration = nodeSortConfiguration,
                sortOrder = sortOrder
            )
        }
    }

    open fun updateNodeSortConfiguration(nodeSortConfiguration: NodeSortConfiguration) {
        viewModelScope.launch {
            runCatching {
                val order = nodeSortConfigurationUiMapper(nodeSortConfiguration)
                setCloudSortOrderUseCase(order)
            }.onFailure {
                Timber.Forest.e(it, "Failed to set cloud sort order")
            }
        }
    }

    fun setItems(nodes: List<TypedNode>, nodesLoadingState: NodesLoadingState) {
        viewModelScope.launch {
            val nodeUiItems = nodeUiItemMapper(
                nodeList = nodes,
                nodeSourceType = args.nodeSourceType,
                existingItems = nodeExplorerSharedUiState.value.items,
            )

            _nodedExplorerSharedUiState.update { state -> state.copy(items = nodeUiItems) }
        }
    }

    fun onNavigateBackEventConsumed() {
        _nodedExplorerSharedUiState.update { state ->
            state.copy(navigateBack = consumed)
        }
    }

    fun updateNodesLoadingState(nodesLoadingState: NodesLoadingState) {
        _nodedExplorerSharedUiState.update { state ->
            state.copy(nodesLoadingState = NodesLoadingState.FullyLoaded)
        }
    }

    abstract fun loadNodes()
    abstract fun refreshNodes()

    data class Args(
        val nodeId: NodeId,
        val nodeSourceType: NodeSourceType,
    )
}