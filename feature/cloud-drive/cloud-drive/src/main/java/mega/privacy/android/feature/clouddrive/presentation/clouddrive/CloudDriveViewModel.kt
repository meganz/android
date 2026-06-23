package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.account.AccountInactivity
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.clouddrive.NodeFetchResult
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetNodeInfoByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.account.AcknowledgeLastPurgeUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountInactivityUseCase
import mega.privacy.android.domain.usecase.account.SuppressPurgeTimestampUseCase
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.folderlink.ContainsMediaItemUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.clouddrive.FetchNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.shares.GetIncomingShareParentUserEmailUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveUiState
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.core.coroutine.takeWhileInclusive
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.shared.nodes.mapper.NodeViewItemMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.TypedNodeItem
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ViewModeGridMenuItemEvent
import mega.privacy.mobile.analytics.event.ViewModeListMenuItemEvent
import timber.log.Timber

/**
 * Cloud drive view model
 *
 * @property getNodeInfoByIdUseCase
 * @property getFileBrowserNodeChildrenUseCase
 * @property setViewTypeUseCase
 * @property monitorViewTypeUseCase
 * @property monitorShowHiddenItemsUseCase
 * @property monitorNodeUpdatesByIdUseCase
 * @property monitorHiddenNodesEnabledUseCase
 * @property nodeViewItemMapper
 * @property getRootNodeIdUseCase
 * @property fetchNodesByIdInChunkUseCase
 * @property setCloudSortOrderUseCase
 * @property nodeSortConfigurationUiMapper
 * @property getContactVerificationWarningUseCase
 * @property areCredentialsVerifiedUseCase
 * @property getIncomingShareParentUserEmailUseCase
 * @property getNodeAccessPermission
 * @property monitorSortCloudOrderUseCase
 * @property containsMediaItemUseCase
 * @property args
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = CloudDriveViewModel.Factory::class)
class CloudDriveViewModel @AssistedInject constructor(
    private val getNodeInfoByIdUseCase: GetNodeInfoByIdUseCase,
    private val getFileBrowserNodeChildrenUseCase: GetFileBrowserNodeChildrenUseCase,
    private val setViewTypeUseCase: SetViewType,
    private val monitorViewTypeUseCase: MonitorViewType,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val nodeViewItemMapper: NodeViewItemMapper,
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
    private val fetchNodesByIdInChunkUseCase: FetchNodesByIdInChunkUseCase,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase,
    private val getIncomingShareParentUserEmailUseCase: GetIncomingShareParentUserEmailUseCase,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
    private val containsMediaItemUseCase: ContainsMediaItemUseCase,
    private val monitorAccountInactivityUseCase: MonitorAccountInactivityUseCase,
    private val acknowledgeLastPurgeUseCase: AcknowledgeLastPurgeUseCase,
    private val suppressPurgeTimestampUseCase: SuppressPurgeTimestampUseCase,
    @Assisted private val args: Args,
) : ViewModel() {

    internal val uiState: StateFlow<CloudDriveUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            stateDataFlow(),
            stateUpdatesFlow(),
            accountInactivityFlow,
        ) { stateData, stateUpdates, accountInactivity ->
            CloudDriveUiState.Data(
                isCloudDriveRoot = args.isRootNode(),
                nodeSourceType = args.nodeSourceType,
                title = stateData.title,
                currentFolderId = stateData.currentFolderId,
                currentViewType = stateUpdates.currentViewType,
                hasWritePermission = stateData.hasWritePermission,
                nodesLoadingState = stateData.loadingState,
                items = stateData.nodeUiItems,
                hasMediaItems = stateData.hasMediaItems,
                showContactNotVerifiedBanner = stateUpdates.showContactNotVerifiedBanner,
                navigateBack = stateUpdates.navigateBackEvent,
                selectedSortOrder = stateUpdates.sortOrder,
                selectedSortConfiguration = stateUpdates.sortConfiguration,
                inactivityMonths = accountInactivity?.inactivityMonths,
                purgeTimestamp = accountInactivity?.purgeTimestamp,
            )

        }.asUiStateFlow(
            scope = viewModelScope,
            initialValue = CloudDriveUiState.Loading(
                title = args.title,
                currentViewType = ViewType.LIST,
                nodeSourceType = args.nodeSourceType,
            )
        )
    }

    private fun stateDataFlow(): Flow<StateData> = currentFolderIdFlow.flatMapLatest { folderId ->
        getHiddenNodesSettingsFlow()
            .map { (isHiddenNodesEnabled, showHiddenNodes) ->
                isHiddenNodesEnabled to (isHiddenNodesEnabled && !showHiddenNodes && !isSharedSourceType) // Hidden nodes are shown in shares screen
            }
            .distinctUntilChanged()
            .flatMapLatest { (isHiddenNodesEnabled, excludeSensitives) ->
                combine(
                    monitorNodeUpdatesFlow.filterNot { it == NodeChanges.Remove }
                        .map { getLatestTitle() to hasWritePermission(folderId) }
                        .onStart { emit(getLatestTitle() to hasWritePermission(folderId)) },
                    fetchNodesByIdInChunkUseCase(folderId, excludeSensitives = excludeSensitives)
                        .catch { Timber.e(it) }
                        .takeWhileInclusive { it.loadingState == NodesLoadingState.PartiallyLoaded }
                        .onCompletion {
                            emitAll(getMonitoredNodesFlow(folderId, excludeSensitives))
                        },
                    flowOf(runCatching { getContactVerificationWarningUseCase() }.getOrDefault(false))
                ) { (title, hasWritePermission), fetchResult, contactVerificationEnabled ->
                    val nodeUiItems = nodeViewItemMapper(
                        nodeList = fetchResult.typedNodes,
                        nodeSourceType = args.nodeSourceType,
                        highlightedNodeId = args.highlightedNodeId,
                        highlightedNames = args.highlightedNodeNames,
                        isHiddenNodesEnabled = isHiddenNodesEnabled,
                        isContactVerificationOn = contactVerificationEnabled,
                    )

                    StateData(
                        currentFolderId = folderId,
                        title = title,
                        hasWritePermission = hasWritePermission,
                        loadingState = fetchResult.loadingState,
                        hasMediaItems = fetchResult.hasMediaItems,
                        nodeUiItems = nodeUiItems
                    )
                }
            }
    }

    private val currentFolderIdFlow: Flow<NodeId> by lazy(LazyThreadSafetyMode.NONE) {
        flow {
            emit(
                if (args.isRootNode()) {
                    getRootNodeIdUseCase() ?: args.currentFolderId
                } else {
                    args.currentFolderId
                }
            )
        }.shareIn(viewModelScope, SharingStarted.WhileSubscribed())
    }

    private fun stateUpdatesFlow(): Flow<StateUpdates> =
        currentFolderIdFlow.flatMapLatest { folderId ->
            combine(
                monitorViewTypeUseCase()
                    .catch { Timber.e(it) },
                monitorFolderUpdatesFlow.mapLatest {
                    shouldShowContactNotVerifiedBanner(folderId)
                },
                monitorSortOrderFlow
                    .map { sortOrder ->
                        sortOrder to nodeSortConfigurationUiMapper(sortOrder)
                    }
                    .onStart { emit(SortOrder.ORDER_DEFAULT_ASC to NodeSortConfiguration.default) },
                deletedFlow
            ) { viewType, shouldShowContactNotVerifiedBanner, (sortOrder, sortConfiguration), navigateBackEvent ->
                StateUpdates(
                    currentViewType = viewType,
                    sortOrder = sortOrder,
                    sortConfiguration = sortConfiguration,
                    navigateBackEvent = navigateBackEvent,
                    showContactNotVerifiedBanner = shouldShowContactNotVerifiedBanner,
                )
            }
        }

    private val monitorNodeUpdatesFlow: SharedFlow<NodeChanges> by lazy(LazyThreadSafetyMode.NONE) {
        monitorNodeUpdatesByIdUseCase(
            nodeId = args.currentFolderId,
            nodeSourceType = args.nodeSourceType
        ).catch { Timber.e(it) }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
    }

    private val backNavigationHandledChannel =
        Channel<Boolean>(onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val deletedFlow = merge(
        monitorNodeUpdatesFlow.filter { it == NodeChanges.Remove }
            .map { triggered },
        backNavigationHandledChannel.receiveAsFlow().map { consumed }
    ).stateIn(viewModelScope, SharingStarted.Eagerly, consumed)

    private val accountInactivityFlow: Flow<AccountInactivity?> =
        monitorAccountInactivityUseCase().catch { Timber.e(it) }

    private val monitorSortOrderFlow: SharedFlow<SortOrder> by lazy(LazyThreadSafetyMode.NONE) {
        monitorSortCloudOrderUseCase()
            .filterNotNull()
            .catch { Timber.e(it) }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
    }

    private val monitorFolderUpdatesFlow by lazy(LazyThreadSafetyMode.NONE) {
        merge(
            monitorNodeUpdatesFlow
                .filterNot { it == NodeChanges.Remove }
                .map { Unit },
            monitorSortOrderFlow.map { Unit }
        ).onStart { emit(Unit) }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed())
    }

    private suspend fun hasWritePermission(
        folderId: NodeId,
    ): Boolean = runCatching {
        val accessPermission = getNodeAccessPermission(folderId)
        accessPermission == AccessPermission.OWNER ||
                accessPermission == AccessPermission.READWRITE ||
                accessPermission == AccessPermission.FULL
    }.onFailure {
        Timber.e(it, "Failed to check write permission")
    }.getOrDefault(false)

    private val isSharedSourceType: Boolean
        get() = args.nodeSourceType == NodeSourceType.INCOMING_SHARES ||
                args.nodeSourceType == NodeSourceType.OUTGOING_SHARES

    private suspend fun getLatestTitle() = runCatching {
        getNodeInfoByIdUseCase(args.currentFolderId)
    }.mapCatching { nodeInfo ->
        if (nodeInfo?.isNodeKeyDecrypted == false) {
            LocalizedText.StringRes(resId = sharedR.string.shared_items_verify_credentials_undecrypted_folder)
        } else {
            LocalizedText.Literal(nodeInfo?.name ?: "")
        }
    }.onFailure {
        Timber.e(it, "Failed to get node name for title update")
    }.getOrDefault(
        LocalizedText.Literal("")
    )

    private fun getHiddenNodesSettingsFlow(): Flow<Pair<Boolean, Boolean>> = combine(
        monitorHiddenNodesEnabledUseCase()
            .catch { Timber.e(it) },
        monitorShowHiddenItemsUseCase()
            .catch { Timber.e(it) },
        ::Pair
    )

    private fun getMonitoredNodesFlow(
        folderId: NodeId,
        excludeSensitives: Boolean,
    ) = monitorFolderUpdatesFlow
        .mapLatest {
            val nodes = getFileBrowserNodeChildrenUseCase(
                parentHandle = folderId.longValue,
                excludeSensitives = excludeSensitives,
            )
            val hasMediaItems = containsMediaItemUseCase(nodes)

            NodeFetchResult(
                loadingState = NodesLoadingState.FullyLoaded,
                hasMediaItems = hasMediaItems,
                typedNodes = nodes
            )
        }

    private suspend fun shouldShowContactNotVerifiedBanner(folderId: NodeId) = runCatching {
        return if (isSharedSourceType
            && args.nodeSourceType == NodeSourceType.INCOMING_SHARES
            && getContactVerificationWarningUseCase()
        ) {
            getIncomingShareParentUserEmailUseCase(folderId)?.let { email ->
                !areCredentialsVerifiedUseCase(email)
            } ?: false
        } else false
    }.onFailure { Timber.e(it) }
        .getOrDefault(false)

    internal fun setCloudSortOrder(sortConfiguration: NodeSortConfiguration) {
        viewModelScope.launch {
            runCatching {
                val order = nodeSortConfigurationUiMapper(sortConfiguration)
                setCloudSortOrderUseCase(order)
            }.onFailure {
                Timber.e(it, "Failed to set cloud sort order")
            }
        }
    }

    /**
     * Process CloudDriveAction and call relevant methods
     * @param action
     */
    fun processAction(action: CloudDriveAction) {
        when (action) {
            is CloudDriveAction.ChangeViewTypeClicked -> onChangeViewTypeClicked(action.newViewType)
            is CloudDriveAction.NavigateBackEventConsumed -> onNavigateBackEventConsumed()
            is CloudDriveAction.InactivityBannerDismissed ->
                onInactivityBannerDismissed(action.purgeTimestamp)
        }
    }

    private fun onInactivityBannerDismissed(purgeTimestamp: Long) {
        // Optimistically hide the banner app-wide for the rest of the session, then acknowledge
        // on the server. On failure the event simply re-fires on the next session.
        suppressPurgeTimestampUseCase(purgeTimestamp)
        viewModelScope.launch {
            runCatching {
                acknowledgeLastPurgeUseCase(purgeTimestamp)
            }.onSuccess {
                Timber.d("InactiveBanner setLastPurgeAcknowledged success, purgeTs=$purgeTimestamp")
            }.onFailure {
                Timber.e(it, "InactiveBanner setLastPurgeAcknowledged failed, purgeTs=$purgeTimestamp")
            }
        }
    }

    private fun onChangeViewTypeClicked(newViewType: ViewType) {
        viewModelScope.launch {
            runCatching {
                setViewTypeUseCase(newViewType)
            }.onFailure {
                Timber.e(it, "Failed to change view type")
            }.onSuccess {
                val event = when (newViewType) {
                    ViewType.LIST -> ViewModeListMenuItemEvent
                    ViewType.GRID -> ViewModeGridMenuItemEvent
                }
                Analytics.tracker.trackEvent(event)
            }
        }
    }

    private fun onNavigateBackEventConsumed() {
        viewModelScope.launch { backNavigationHandledChannel.send(true) }
    }

    /**
     * Factory
     */
    @AssistedFactory
    interface Factory {
        /**
         * Create
         *
         * @param args
         * @return CloudDriveViewModel instance
         */
        fun create(args: Args): CloudDriveViewModel
    }

    /**
     * Args
     *
     * @property currentFolderId
     * @property title
     * @property nodeSourceType
     * @property highlightedNodeId
     * @property highlightedNodeNames
     *
     */
    data class Args(
        val currentFolderId: NodeId,
        val title: LocalizedText,
        val nodeSourceType: NodeSourceType,
        val highlightedNodeId: NodeId?,
        val highlightedNodeNames: List<String>?,
    ) {
        /**
         * Is root node
         */
        fun isRootNode() = currentFolderId.longValue == -1L
    }
}

/**
 * State data
 *
 * @property currentFolderId
 * @property title
 * @property hasWritePermission
 * @property loadingState
 * @property hasMediaItems
 * @property nodeUiItems
 */
private data class StateData(
    val currentFolderId: NodeId,
    val title: LocalizedText,
    val hasWritePermission: Boolean,
    val loadingState: NodesLoadingState,
    val hasMediaItems: Boolean,
    val nodeUiItems: List<TypedNodeItem<TypedNode>>,
)

/**
 * State updates
 *
 * @property sortOrder
 * @property sortConfiguration
 * @property navigateBackEvent
 * @property currentViewType
 * @property showContactNotVerifiedBanner
 */
private data class StateUpdates(
    val sortOrder: SortOrder,
    val sortConfiguration: NodeSortConfiguration,
    val navigateBackEvent: StateEvent,
    val currentViewType: ViewType,
    val showContactNotVerifiedBanner: Boolean,
)
