package mega.privacy.android.feature.clouddrive.presentation.folderbrowser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.coroutine.logAndSwallowExceptions
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetNodeInfoByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.folderpreference.MonitorFolderSortOrderUseCase
import mega.privacy.android.domain.usecase.folderpreference.MonitorFolderViewTypeUseCase
import mega.privacy.android.domain.usecase.folderpreference.SetFolderSortOrderUseCase
import mega.privacy.android.domain.usecase.folderpreference.SetFolderViewTypeUseCase
import mega.privacy.android.domain.usecase.node.GetChildrenCountUseCase
import mega.privacy.android.domain.usecase.node.HandleToBase64UseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.clouddrive.GetNodeChildrenPageUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.shares.GetIncomingShareParentUserEmailUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.folderbrowser.FolderBrowserViewModel.Companion.HIGHLIGHT_PAGE_CAP
import mega.privacy.android.feature.clouddrive.presentation.folderbrowser.model.FolderBrowserAction
import mega.privacy.android.feature.clouddrive.presentation.folderbrowser.model.FolderBrowserUiState
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.shared.nodes.mapper.NodeViewItemMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.paging.NodeListPager
import mega.privacy.android.shared.nodes.paging.PagedLoadState
import mega.privacy.android.shared.nodes.paging.PagedNodes
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ViewModeGridMenuItemEvent
import mega.privacy.mobile.analytics.event.ViewModeListMenuItemEvent
import timber.log.Timber

/**
 * Browses one folder page by page.
 *
 * One [NodeListPager] per (sort order, hidden nodes) key: a key change cancels the pager session
 * and starts a new pager at page 0. Settings that never change while the folder is open are read
 * once up front; title, permission, banner and view type are their own subscriptions writing their
 * own fields of [uiState].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = FolderBrowserViewModel.Factory::class)
class FolderBrowserViewModel @AssistedInject constructor(
    private val getNodeInfoByIdUseCase: GetNodeInfoByIdUseCase,
    private val getNodeChildrenPageUseCase: GetNodeChildrenPageUseCase,
    private val getChildrenCountUseCase: GetChildrenCountUseCase,
    private val setViewTypeUseCase: SetViewType,
    private val monitorViewTypeUseCase: MonitorViewType,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val nodeViewItemMapper: NodeViewItemMapper,
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase,
    private val getIncomingShareParentUserEmailUseCase: GetIncomingShareParentUserEmailUseCase,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase,
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
    private val handleToBase64UseCase: HandleToBase64UseCase,
    private val monitorFolderViewTypeUseCase: MonitorFolderViewTypeUseCase,
    private val monitorFolderSortOrderUseCase: MonitorFolderSortOrderUseCase,
    private val setFolderViewTypeUseCase: SetFolderViewTypeUseCase,
    private val setFolderSortOrderUseCase: SetFolderSortOrderUseCase,
    private val savedStateHandle: SavedStateHandle,
    @Assisted private val args: Args,
) : ViewModel() {

    val uiState: StateFlow<FolderBrowserUiState>
        field = MutableStateFlow(
            FolderBrowserUiState(
                title = args.title,
                nodeSourceType = args.nodeSourceType,
                isCloudDriveRoot = args.isRootNode(),
            )
        )

    private val visibleRanges = NodeListPager.visibleRanges()

    private val nodeUpdates: SharedFlow<NodeChanges> = monitorNodeUpdatesByIdUseCase(
        nodeId = args.currentFolderId,
        nodeSourceType = args.nodeSourceType,
    )
        .catch { Timber.e(it) }
        .shareIn(viewModelScope, SharingStarted.Eagerly)

    private val folderChanges: Flow<Unit> = nodeUpdates
        .filterNot { it == NodeChanges.Remove }
        .map { }

    private val isSharedSourceType: Boolean
        get() = args.nodeSourceType == NodeSourceType.INCOMING_SHARES ||
                args.nodeSourceType == NodeSourceType.OUTGOING_SHARES

    init {
        viewModelScope.launch { observeFolder() }
    }

    private suspend fun observeFolder(): Unit = coroutineScope {
        val folderId = resolveFolderId()
        val isNodeInBackups = isNodeInBackups(folderId)
        uiState.update { it.copy(currentFolderId = folderId, isNodeInBackups = isNodeInBackups) }
        val folderKey = handleToBase64UseCase(folderId.longValue)
        // Only drives the verified badge of incoming shares, so Cloud Drive never asks the SDK
        val isContactVerificationOn = isSharedSourceType && runCatching {
            getContactVerificationWarningUseCase()
        }.logAndSwallowExceptions().getOrDefault(false)
        val sortOrders = monitorFolderSortOrderUseCase(
            folderKey = folderKey,
            orElse = monitorSortCloudOrderUseCase().filterNotNull(),
        ).catch { Timber.e(it) }

        launch {
            folderChanges.onStart { emit(Unit) }.collectLatest {
                refreshFolderDetails(folderId, isContactVerificationOn)
            }
        }
        launch {
            monitorFolderViewTypeUseCase(folderKey = folderKey, orElse = monitorViewTypeUseCase())
                .catch { Timber.e(it) }
                .collect { viewType -> uiState.update { it.copy(currentViewType = viewType) } }
        }
        launch {
            nodeUpdates.filter { it == NodeChanges.Remove }.collect {
                uiState.update { it.copy(navigateBack = triggered) }
            }
        }

        // After process death the first session reloads as many rows as were loaded before, so the
        // list's restored scroll position has items under it; a later key change starts at page 0
        val restoredCount = savedStateHandle.get<Int>(LOADED_COUNT_KEY) ?: 0
        pagerKeys(sortOrders).withIndex().collectLatest { (index, key) ->
            val initialCount = if (index == 0) restoredCount else 0
            runPagerSession(folderId, key, initialCount, isContactVerificationOn)
        }
    }

    private suspend fun resolveFolderId(): NodeId = if (args.isRootNode()) {
        runCatching { getRootNodeIdUseCase() }
            .logAndSwallowExceptions()
            .getOrNull() ?: args.currentFolderId
    } else {
        args.currentFolderId
    }

    private suspend fun refreshFolderDetails(folderId: NodeId, isContactVerificationOn: Boolean) {
        val title = getLatestTitle(folderId)
        val hasWritePermission = hasWritePermission(folderId)
        val showContactNotVerifiedBanner =
            shouldShowContactNotVerifiedBanner(folderId, isContactVerificationOn)
        uiState.update {
            it.copy(
                title = title,
                hasWritePermission = hasWritePermission,
                showContactNotVerifiedBanner = showContactNotVerifiedBanner,
            )
        }
    }

    private fun pagerKeys(sortOrders: Flow<SortOrder>): Flow<PagerKey> {
        val hiddenNodesSettings = combine(
            monitorHiddenNodesEnabledUseCase().catch { Timber.e(it) },
            monitorShowHiddenItemsUseCase().catch { Timber.e(it) },
        ) { isHiddenNodesEnabled, showHiddenNodes ->
            // Hidden nodes are shown in the shares screens
            isHiddenNodesEnabled to (isHiddenNodesEnabled && !showHiddenNodes && !isSharedSourceType)
        }
        return combine(
            sortOrders,
            hiddenNodesSettings
        ) { sortOrder, (isHiddenNodesEnabled, excludeSensitives) ->
            PagerKey(sortOrder, isHiddenNodesEnabled, excludeSensitives)
        }.distinctUntilChanged()
    }

    /**
     * One pager for one key: runs it, mirrors its state, and suspends until the session is
     * cancelled by the next key
     */
    private suspend fun runPagerSession(
        folderId: NodeId,
        key: PagerKey,
        initialCount: Int,
        isContactVerificationOn: Boolean,
    ): Unit = coroutineScope {
        uiState.update {
            it.copy(
                selectedSortOrder = key.sortOrder,
                selectedSortConfiguration = nodeSortConfigurationUiMapper(key.sortOrder),
            )
        }

        val pager = createPager(
            folderId = folderId,
            key = key,
            initialCount = initialCount,
            isContactVerificationOn = isContactVerificationOn
        )

        launch { pager.run(folderChanges, visibleRanges, observers = uiState.subscriptionCount) }

        var searchingHighlight =
            args.highlightedNodeId != null || !args.highlightedNodeNames.isNullOrEmpty()

        // TODO AND-25306: set hasMediaItems from the SDK's getMediaPresence once per loaded prefix
        //  (first page and each re-read), async with the page request
        // A new pager starts empty; the previous items stay until its first page lands
        pager.state.filterNot { it.loadState == PagedLoadState.FirstPage }.collect { paged ->
            if (paged.loadState == PagedLoadState.Idle) {
                if (searchingHighlight) searchingHighlight = pager.loadMoreForHighlight(paged)
                savedStateHandle[LOADED_COUNT_KEY] = paged.items.size
            }
            uiState.update { it.copy(paged = paged) }
        }
    }

    private fun createPager(
        folderId: NodeId,
        key: PagerKey,
        initialCount: Int,
        isContactVerificationOn: Boolean,
    ) = NodeListPager(
        pageSize = PAGE_SIZE,
        initialCount = initialCount,
        load = { page ->
            getNodeChildrenPageUseCase(
                nodeId = folderId,
                sortOrder = key.sortOrder,
                excludeSensitives = key.excludeSensitives,
                page = page,
            )
        },
        map = { nodes ->
            nodeViewItemMapper(
                nodeList = nodes,
                nodeSourceType = args.nodeSourceType,
                highlightedNodeId = args.highlightedNodeId,
                highlightedNames = args.highlightedNodeNames,
                isHiddenNodesEnabled = key.isHiddenNodesEnabled,
                isContactVerificationOn = isContactVerificationOn,
            )
        },
        // The SDK count ignores the sensitivity filter, so it only matches the list unfiltered
        count = if (key.excludeSensitives) null else ({ getChildrenCountUseCase(folderId) }),
    )

    /**
     * Requests the next page while the highlighted node is not in the list, up to
     * [HIGHLIGHT_PAGE_CAP] pages; past that the search stops silently, as "Locate" does today for
     * a node it cannot find
     *
     * @return whether the search goes on
     */
    private fun NodeListPager<TypedNode>.loadMoreForHighlight(
        paged: PagedNodes<TypedNode>,
    ): Boolean {
        val found = paged.items.any { it.isHighlighted }
        val exhausted = !paged.hasMore || paged.items.size >= HIGHLIGHT_PAGE_CAP * PAGE_SIZE
        if (found || exhausted) return false
        ensureLoaded(paged.items.size + PAGE_SIZE - 1)
        return true
    }

    private suspend fun hasWritePermission(folderId: NodeId): Boolean = runCatching {
        when (getNodeAccessPermission(folderId)) {
            AccessPermission.OWNER, AccessPermission.READWRITE, AccessPermission.FULL -> true
            else -> false
        }
    }.logAndSwallowExceptions().getOrDefault(false)

    private suspend fun isNodeInBackups(folderId: NodeId): Boolean = runCatching {
        isNodeInBackupsUseCase(folderId.longValue)
    }.logAndSwallowExceptions().getOrDefault(false)

    /**
     * The folder's current name; the title already shown stays when the node cannot be read
     */
    private suspend fun getLatestTitle(folderId: NodeId): LocalizedText = runCatching {
        getNodeInfoByIdUseCase(folderId)
    }.logAndSwallowExceptions().getOrNull()?.let { nodeInfo ->
        if (nodeInfo.isNodeKeyDecrypted) {
            LocalizedText.Literal(nodeInfo.name)
        } else {
            LocalizedText.StringRes(resId = sharedR.string.shared_items_verify_credentials_undecrypted_folder)
        }
    } ?: uiState.value.title

    private suspend fun shouldShowContactNotVerifiedBanner(
        folderId: NodeId,
        isContactVerificationOn: Boolean,
    ) = runCatching {
        if (args.nodeSourceType == NodeSourceType.INCOMING_SHARES && isContactVerificationOn) {
            getIncomingShareParentUserEmailUseCase(folderId)?.let { email ->
                !areCredentialsVerifiedUseCase(email)
            } ?: false
        } else {
            false
        }
    }.logAndSwallowExceptions().getOrDefault(false)

    internal fun setCloudSortOrder(sortConfiguration: NodeSortConfiguration) {
        viewModelScope.launch {
            runCatching {
                setFolderSortOrderUseCase(
                    folderKey = handleToBase64UseCase(currentFolderId().longValue),
                    sortOrder = nodeSortConfigurationUiMapper(sortConfiguration),
                    currentViewType = uiState.value.currentViewType,
                    orElse = { setCloudSortOrderUseCase(it) },
                )
            }.onFailure {
                Timber.e(it, "Failed to set cloud sort order")
            }
        }
    }

    private fun currentFolderId(): NodeId = uiState.value.currentFolderId ?: args.currentFolderId

    /**
     * @param action the UI action to handle
     */
    fun processAction(action: FolderBrowserAction) {
        when (action) {
            is FolderBrowserAction.ChangeViewTypeClicked -> onChangeViewTypeClicked(action.newViewType)
            is FolderBrowserAction.NavigateBackEventConsumed ->
                uiState.update { it.copy(navigateBack = consumed) }

            is FolderBrowserAction.VisibleRangeChanged -> visibleRanges.tryEmit(action.range)
        }
    }

    private fun onChangeViewTypeClicked(newViewType: ViewType) {
        viewModelScope.launch {
            runCatching {
                setFolderViewTypeUseCase(
                    folderKey = handleToBase64UseCase(currentFolderId().longValue),
                    viewType = newViewType,
                    currentSortOrder = uiState.value.selectedSortOrder,
                    orElse = { setViewTypeUseCase(it) },
                )
            }.onFailure {
                Timber.e(it, "Failed to change view type")
            }.onSuccess {
                Analytics.tracker.trackEvent(
                    when (newViewType) {
                        ViewType.LIST -> ViewModeListMenuItemEvent
                        ViewType.GRID -> ViewModeGridMenuItemEvent
                    }
                )
            }
        }
    }

    private data class PagerKey(
        val sortOrder: SortOrder,
        val isHiddenNodesEnabled: Boolean,
        val excludeSensitives: Boolean,
    )

    /**
     * Factory
     */
    @AssistedFactory
    interface Factory {
        /**
         * @param args the folder to browse
         */
        fun create(args: Args): FolderBrowserViewModel
    }

    /**
     * @property currentFolderId the folder to browse, -1 for the Cloud Drive root
     * @property title the title to show until the folder name is known
     * @property nodeSourceType
     * @property highlightedNodeId the node to scroll to, if any
     * @property highlightedNodeNames the node names to highlight, if any
     */
    data class Args(
        val currentFolderId: NodeId,
        val title: LocalizedText,
        val nodeSourceType: NodeSourceType,
        val highlightedNodeId: NodeId?,
        val highlightedNodeNames: List<String>?,
    ) {
        fun isRootNode() = currentFolderId.longValue == -1L
    }

    companion object {
        internal const val PAGE_SIZE = 500
        internal const val HIGHLIGHT_PAGE_CAP = 20
        internal const val LOADED_COUNT_KEY = "folder_browser_loaded_count"
    }
}
