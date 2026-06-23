package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.SearchParameters
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.GetNodeNavigationStackUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.search.SearchUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.shared.nodes.mapper.NodeSourceTypeToSearchTargetMapper
import mega.privacy.android.shared.nodes.mapper.NodeViewItemMapper
import mega.privacy.android.shared.nodes.model.NodeViewItem
import timber.log.Timber

/**
 * Shared ViewModel for node explorer screens (cloud, incoming shares, favourites). Builds the whole
 * [uiState] reactively from the source's [nodesFlow] plus the shared monitors; subclasses only
 * supply how nodes are fetched plus the folder title/root flags.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class NodeExplorerSharedViewModel(
    monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    monitorStorageStateUseCase: MonitorStorageStateUseCase,
    monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val nodeViewItemMapper: NodeViewItemMapper,
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    private val searchUseCase: SearchUseCase,
    private val nodeSourceTypeToSearchTargetMapper: NodeSourceTypeToSearchTargetMapper,
    private val getNodeNavigationStackUseCase: GetNodeNavigationStackUseCase,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val args: Args,
) : ViewModel() {

    /** Raw nodes for this source, with the current loading progress. */
    protected abstract val nodesFlow: Flow<NodesResult>

    /** The folder title for this source. Non-cloud sources emit a blank/source-level title. */
    protected abstract val folderNameFlow: Flow<LocalizedText>

    /** Whether the current folder is the source root. Non-cloud sources emit `true`. */
    protected abstract val isRootNodeFlow: Flow<Boolean>

    /** Sources that need a manual refetch on SDK node updates opt in; reactive sources leave `false`. */
    protected open val monitorsNodeUpdates: Boolean = true

    private val searchQueryChannel = Channel<String?>(Channel.CONFLATED)
    private val navigateBackChannel = Channel<StateEvent>(Channel.CONFLATED)
    private val noConnectionChannel = Channel<StateEvent>(Channel.CONFLATED)
    private val manualRefreshChannel = Channel<Unit>(Channel.CONFLATED)

    private val nodeUpdates: Flow<NodeChanges> by lazy {
        if (monitorsNodeUpdates) {
            monitorNodeUpdatesByIdUseCase(args.nodeId, args.nodeSourceType)
                .catch { Timber.e(it) }
        } else {
            emptyFlow()
        }
    }

    /**
     * A tick subclasses fold into [nodesFlow] to refetch — fired by pull-to-refresh and by non-removal
     * SDK node updates. Node removals route to [navigateBackFlow] instead. Lazy so it is wired after
     * subclass construction (it reads the overridable [monitorsNodeUpdates]).
     */
    protected val refreshSignal: Flow<Unit> by lazy {
        merge(
            manualRefreshChannel.receiveAsFlow(),
            nodeUpdates
                .onEach { if (it == NodeChanges.Remove) navigateBackChannel.trySend(triggered) }
                .filter { it != NodeChanges.Remove }
                .map { },
        )
    }

    private val hiddenEnabledFlow = monitorHiddenNodesEnabledUseCase()
        .catch { Timber.e(it) }
        .onStart { emit(false) }

    private val showHiddenFlow = monitorShowHiddenItemsUseCase()
        .catch { Timber.e(it) }
        .onStart { emit(false) }

    private val storageOverQuotaFlow = monitorStorageStateUseCase()
        .map { it == StorageState.Red || it == StorageState.PayWall }
        .catch { Timber.e(it) }
        .onStart { emit(false) }

    private val connectivityFlow = monitorConnectivityUseCase()
        .catch { Timber.e(it) }
        .withIndex()
        .onEach { (index, isConnected) ->
            if (index == 0 && !isConnected) noConnectionChannel.trySend(triggered)
        }
        .map { it.value }
        .onStart { emit(true) }

    private val noConnectionFlow = noConnectionChannel.receiveAsFlow().onStart { emit(consumed) }
    private val navigateBackFlow = navigateBackChannel.receiveAsFlow().onStart { emit(consumed) }

    private val searchFlow: Flow<SearchState> = combine(
        searchQueryChannel.receiveAsFlow()
            .onStart { emit(null) }
            .distinctUntilChanged()
            .flatMapLatest { query -> searchResults(query) },
        hiddenEnabledFlow,
    ) { result, isHiddenNodesEnabled ->
        SearchState(
            items = if (result.loadingState == NodesLoadingState.Loading) {
                emptyList()
            } else {
                mapNodes(result.nodes, isHiddenNodesEnabled)
            },
            loadingState = result.loadingState,
            query = result.query,
        )
    }

    private val mappedItemsFlow: Flow<MappedItems> by lazy {
        combine(
            nodesFlow.onStart { emit(NodesResult(emptyList(), NodesLoadingState.Loading)) },
            hiddenEnabledFlow,
        ) { result, isHiddenNodesEnabled ->
            MappedItems(
                items = mapNodes(result.nodes, isHiddenNodesEnabled),
                loadingState = result.loadingState,
            )
        }
    }

    private val folderInfoFlow: Flow<FolderInfo> by lazy {
        combine(folderNameFlow, isRootNodeFlow) { folderName, isRoot ->
            FolderInfo(
                folderName,
                isRoot
            )
        }
    }

    private val globalFlow: Flow<Global> = combine(
        hiddenEnabledFlow,
        showHiddenFlow,
        storageOverQuotaFlow,
        connectivityFlow,
    ) { isHiddenNodesEnabled, showHiddenNodes, isStorageOverQuota, isConnected ->
        Global(isHiddenNodesEnabled, showHiddenNodes, isStorageOverQuota, isConnected)
    }

    private val eventsFlow: Flow<Events> = combine(
        navigateBackFlow,
        noConnectionFlow,
    ) { navigateBack, noConnectionEvent -> Events(navigateBack, noConnectionEvent) }

    val uiState: StateFlow<NodeExplorerUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            mappedItemsFlow,
            searchFlow,
            folderInfoFlow,
            globalFlow,
            eventsFlow,
        ) { items, search, folderInfo, global, events ->
            if (items.loadingState == NodesLoadingState.Loading) {
                NodeExplorerUiState.Loading
            } else {
                NodeExplorerUiState.Data(
                    currentFolderId = args.nodeId,
                    nodeSourceType = args.nodeSourceType,
                    items = items.items,
                    nodesLoadingState = items.loadingState,
                    searchItems = search.items,
                    searchLoadingState = search.loadingState,
                    searchedQuery = search.query,
                    showHiddenNodes = global.showHiddenNodes,
                    isHiddenNodesEnabled = global.isHiddenNodesEnabled,
                    isStorageOverQuota = global.isStorageOverQuota,
                    isConnected = global.isConnected,
                    navigateBack = events.navigateBack,
                    noConnectionEvent = events.noConnectionEvent,
                    folderName = folderInfo.folderName,
                    isRoot = folderInfo.isRoot,
                )
            }
        }.asUiStateFlow(viewModelScope, NodeExplorerUiState.Loading)
    }

    private suspend fun mapNodes(
        nodes: List<TypedNode>,
        isHiddenNodesEnabled: Boolean,
    ): List<NodeViewItem<TypedNode>> = nodeViewItemMapper(
        nodeList = nodes,
        nodeSourceType = args.nodeSourceType,
        highlightedNodeId = null,
        isHiddenNodesEnabled = isHiddenNodesEnabled,
        highlightedNames = null,
        isContactVerificationOn = contactVerificationEnabled(),
    )

    private suspend fun contactVerificationEnabled() =
        runCatching { getContactVerificationWarningUseCase() }.getOrDefault(false)

    private fun searchResults(query: String?): Flow<SearchResult> = when {
        query.isNullOrBlank() -> flowOf(
            SearchResult(
                emptyList(),
                NodesLoadingState.FullyLoaded,
                query
            )
        )

        else -> flow {
            emit(SearchResult(emptyList(), NodesLoadingState.Loading, query))
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
            emit(SearchResult(nodes, NodesLoadingState.FullyLoaded, query))
        }
    }

    /**
     * Sets the query whose matches [NodeExplorerUiState.Data.searchItems] exposes. Pass `null`/blank
     * when the search is closed. Re-issuing the same query is ignored ([distinctUntilChanged]) so a
     * configuration change keeps the previous results.
     */
    fun onSearchQuery(query: String?) {
        viewModelScope.launch { searchQueryChannel.send(query) }
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

    /** Pull-to-refresh: refetches this source's nodes through [refreshSignal]. */
    fun refreshNodes() {
        manualRefreshChannel.trySend(Unit)
    }

    fun onNavigateBackEventConsumed() {
        navigateBackChannel.trySend(consumed)
    }

    fun onNoConnectionEventConsumed() {
        noConnectionChannel.trySend(consumed)
    }

    data class Args(
        val nodeId: NodeId,
        val nodeSourceType: NodeSourceType,
    )

    private data class MappedItems(
        val items: List<NodeViewItem<TypedNode>>,
        val loadingState: NodesLoadingState,
    )

    private data class SearchResult(
        val nodes: List<TypedNode>,
        val loadingState: NodesLoadingState,
        val query: String?,
    )

    private data class SearchState(
        val items: List<NodeViewItem<TypedNode>>,
        val loadingState: NodesLoadingState,
        val query: String?,
    )

    private data class Global(
        val isHiddenNodesEnabled: Boolean,
        val showHiddenNodes: Boolean,
        val isStorageOverQuota: Boolean,
        val isConnected: Boolean,
    )

    private data class FolderInfo(
        val folderName: LocalizedText,
        val isRoot: Boolean,
    )

    private data class Events(
        val navigateBack: StateEvent,
        val noConnectionEvent: StateEvent,
    )

    data class NodesResult(
        val nodes: List<TypedNode>,
        val loadingState: NodesLoadingState,
    )
}
