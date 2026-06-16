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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.SearchParameters
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.node.GetNodeNavigationStackUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.search.SearchUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.shared.nodes.mapper.NodeSourceTypeToSearchTargetMapper
import mega.privacy.android.shared.nodes.mapper.NodeViewItemMapper
import timber.log.Timber

/**
 * Shared ViewModel for node explorer screens (cloud, incoming shares, favourites).
 */
abstract class NodeExplorerSharedViewModel(
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    private val monitorStorageStateUseCase: MonitorStorageStateUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val nodeViewItemMapper: NodeViewItemMapper,
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    private val searchUseCase: SearchUseCase,
    private val nodeSourceTypeToSearchTargetMapper: NodeSourceTypeToSearchTargetMapper,
    private val getNodeNavigationStackUseCase: GetNodeNavigationStackUseCase,
    private val args: Args,
) : ViewModel() {

    private val _nodedExplorerSharedUiState = MutableStateFlow(NodesExplorerSharedUiState())
    val nodeExplorerSharedUiState = _nodedExplorerSharedUiState.asStateFlow()

    private val searchQuery = MutableStateFlow<String?>(null)

    init {
        _nodedExplorerSharedUiState.update { state ->
            state.copy(
                currentFolderId = args.nodeId,
                nodeSourceType = args.nodeSourceType
            )
        }
        monitorHiddenNodes()
        monitorStorageOverQuota()
        monitorSearchQuery()
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

    protected fun setItems(
        nodes: List<TypedNode>, nodesLoadingState: NodesLoadingState,
    ) {
        viewModelScope.launch {
            val nodeUiItems = nodeViewItemMapper(
                nodeList = nodes,
                nodeSourceType = args.nodeSourceType,
                highlightedNodeId = null,
                isHiddenNodesEnabled = _nodedExplorerSharedUiState.value.isHiddenNodesEnabled,
                highlightedNames = null,
                isContactVerificationOn = contactVerificationEnabled(),
            )

            _nodedExplorerSharedUiState.update { state ->
                state.copy(
                    items = nodeUiItems,
                    nodesLoadingState = nodesLoadingState,
                )
            }
        }
    }

    private suspend fun contactVerificationEnabled() =
        runCatching { getContactVerificationWarningUseCase() }.getOrDefault(false)

    /**
     * Sets the query whose matches [NodesExplorerSharedUiState.searchItems] exposes. Pass `null`/blank
     * when the search is closed. Re-issuing the same query is ignored (the backing [StateFlow] only
     * emits on change) so a configuration change keeps the previous results.
     */
    fun onSearchQuery(query: String?) {
        if (!query.isNullOrBlank() && query != _nodedExplorerSharedUiState.value.searchedQuery) {
            _nodedExplorerSharedUiState.update { state ->
                state.copy(searchLoadingState = NodesLoadingState.Loading)
            }
        }
        searchQuery.value = query
    }

    /**
     * Runs the search for the latest [searchQuery]; [collectLatest] cancels the previous search when
     * a new query arrives.
     */
    private fun monitorSearchQuery() {
        viewModelScope.launch {
            searchQuery.filterNotNull().collectLatest { search(it) }
        }
    }

    /**
     * Resolves the folder path (top-down, including [nodeId]) to push so navigating to a search
     * result reproduces its hierarchy and Back walks the real directory tree instead of jumping to
     * the root.
     */
    suspend fun resolveSearchResultStack(nodeId: NodeId): List<NodeId> =
        runCatching { getNodeNavigationStackUseCase(nodeId).stack }
            .onFailure { Timber.e(it) }
            .getOrNull()
            .orEmpty()
            .ifEmpty { listOf(nodeId) }

    /**
     * Runs a recursive search for [query] scoped to this source ([Args.nodeId]/[Args.nodeSourceType])
     * and folds the matches into [NodesExplorerSharedUiState.searchItems], kept separate from
     * [NodesExplorerSharedUiState.items] so the browse list stays unfiltered.
     */
    private suspend fun search(query: String?) {
        if (query.isNullOrBlank()) {
            _nodedExplorerSharedUiState.update { state ->
                state.copy(
                    searchItems = emptyList(),
                    searchLoadingState = NodesLoadingState.FullyLoaded,
                    searchedQuery = query,
                )
            }
            return
        }
        val nodes = runCatching {
            searchUseCase(
                parentHandle = args.nodeId,
                nodeSourceType = args.nodeSourceType,
                searchParameters = SearchParameters(
                    query = query,
                    searchTarget = nodeSourceTypeToSearchTargetMapper(args.nodeSourceType),
                    description = query,
                ),
            )
        }.onFailure { Timber.e(it) }.getOrDefault(emptyList())
        val items = nodeViewItemMapper(
            nodeList = nodes,
            nodeSourceType = args.nodeSourceType,
            highlightedNodeId = null,
            isHiddenNodesEnabled = _nodedExplorerSharedUiState.value.isHiddenNodesEnabled,
            highlightedNames = null,
            isContactVerificationOn = contactVerificationEnabled(),
        )
        _nodedExplorerSharedUiState.update { state ->
            state.copy(
                searchItems = items,
                searchLoadingState = NodesLoadingState.FullyLoaded,
                searchedQuery = query,
            )
        }
    }

    fun onNavigateBackEventConsumed() {
        _nodedExplorerSharedUiState.update { state ->
            state.copy(navigateBack = consumed)
        }
    }

    abstract fun loadNodes()
    abstract fun refreshNodes()

    data class Args(
        val nodeId: NodeId,
        val nodeSourceType: NodeSourceType,
    )
}
