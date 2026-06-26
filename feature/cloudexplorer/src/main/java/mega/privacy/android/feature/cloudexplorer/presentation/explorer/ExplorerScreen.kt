package mega.privacy.android.feature.cloudexplorer.presentation.explorer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.button.InlineAnchoredButtonGroup
import mega.android.core.ui.components.tabs.MegaCollapsibleTabRow
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerTab
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.rememberChatExplorerSelectionState
import mega.privacy.android.feature.cloudexplorer.presentation.components.visibleNodeItems
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.extensions.actionStringId
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.extensions.titleStringId
import mega.privacy.android.feature.cloudexplorer.presentation.favouritesexplorer.FavouritesExplorerTab
import mega.privacy.android.feature.cloudexplorer.presentation.incomingsharesexplorer.IncomingExplorerTab
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.CloudExplorerTab
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.navigation.contract.menu.NewFolderMenuAction
import mega.privacy.android.navigation.destination.ExplorerNavKey
import mega.privacy.android.navigation.destination.NewFolderDialogNavKey
import mega.privacy.android.navigation.destination.NodesExplorerNavKey
import mega.privacy.android.navigation.destination.ShareTextToMegaNavKey
import mega.privacy.android.shared.nodes.dialog.newfolder.rememberNewFolderResult
import mega.privacy.android.shared.nodes.model.NodeViewItem
import mega.privacy.android.shared.nodes.selection.rememberNodeSelectionState
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.search.presentation.component.SearchTopAppBar
import mega.privacy.mobile.analytics.core.event.identifier.EventIdentifier
import mega.privacy.mobile.analytics.event.CloudExplorerCancelButtonPressedEvent
import mega.privacy.mobile.analytics.event.CloudExplorerCloseButtonPressedEvent
import mega.privacy.mobile.analytics.event.CloudExplorerConfirmedChatButtonPressedEvent
import mega.privacy.mobile.analytics.event.CloudExplorerConfirmedCloudButtonPressedEvent
import mega.privacy.mobile.analytics.event.CloudExplorerConfirmedFavouritesButtonPressedEvent
import mega.privacy.mobile.analytics.event.CloudExplorerConfirmedIncomingButtonPressedEvent
import mega.privacy.mobile.analytics.event.CloudExplorerConfirmedSearchButtonPressedEvent
import mega.privacy.mobile.analytics.event.CloudExplorerScreenEvent
import mega.privacy.mobile.analytics.event.CloudExplorerSearchButtonPressedEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExplorerScreen(
    explorerMode: ExplorerMode,
    startNavKey: ExplorerNavKey,
    isInnerNavigation: Boolean,
    nodeExplorerId: NodeId,
    nodeSourceType: NodeSourceType,
    onCloseExplorerScreen: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    isProcessingAction: Boolean,
    modifier: Modifier = Modifier,
    shareUris: List<UriPath>? = null,
    tabIndex: Int = CLOUD_TAB_INDEX,
    disabledTargetId: NodeId? = null,
    disabledNodeIds: Set<NodeId> = emptySet(),
    pickerRestrictions: ExplorerPickerRestrictions? = null,
    onFolderPicked: (NodeId) -> Unit = {},
    onFilesPicked: (List<NodeId>) -> Unit = {},
    onChatsSelected: () -> Unit = {},
    prepareChatsEvent: StateEvent = consumed,
    onPrepareChatsConsumed: () -> Unit = {},
    onChatsReadyToShare: (List<Long>) -> Unit = {},
    monitorResult: (String) -> Flow<Any?> = { emptyFlow() },
    clearResult: (String) -> Unit = {},
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(tabIndex) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    val onSearchQueryChanged: (String) -> Unit = { searchText = it }
    val onCloseSearch: () -> Unit = {
        searchText = ""
        showSearch = false
    }
    val protectedUserTap: (() -> Unit) -> Unit = { action -> if (!isProcessingAction) action() }
    val viewModel = hiltViewModel<ExplorerViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackBarHostState.current
    val resources = LocalResources.current
    val showNoConnectionSnackbar: () -> Unit = {
        coroutineScope.launch {
            snackbarHostState?.showAutoDurationSnackbar(
                resources.getString(sharedR.string.error_no_internet_title)
            )
        }
    }
    val onConnectedNavigate: (NavKey) -> Unit = { navKey ->
        if (uiState.isConnected) onNavigate(navKey) else showNoConnectionSnackbar()
    }
    val chatExplorerSelectionState = rememberChatExplorerSelectionState()
    val nodeSelectionState = rememberNodeSelectionState()
    val hasSelection = when {
        !explorerMode.isFolderPicker -> nodeSelectionState.isInSelectionMode
        selectedTabIndex == CHAT_TAB_INDEX -> chatExplorerSelectionState.isInSelectionMode
        else -> false
    }
    val selectedItemsCount = when {
        !explorerMode.isFolderPicker -> (nodeSelectionState.selectedNodeIds - disabledNodeIds).size
        selectedTabIndex == CHAT_TAB_INDEX -> chatExplorerSelectionState.selectedItemsCount
        else -> 0
    }
    val clearSelection: () -> Unit = {
        nodeSelectionState.deselectAll()
        chatExplorerSelectionState.deselectAll()
    }
    val isAllSelected = uiState.selectableNodeIds.all { it in nodeSelectionState.selectedNodeIds }

    if (!isInnerNavigation) {
        LaunchedOnceEffect {
            Analytics.tracker.trackEvent(CloudExplorerScreenEvent)
        }
    }

    LaunchedEffect(selectedTabIndex) { viewModel.onTabSelected(selectedTabIndex) }

    EventEffect(
        event = uiState.noConnectionEvent,
        onConsumed = viewModel::onNoConnectionEventConsumed,
    ) { showNoConnectionSnackbar() }

    // Keep "select all" extending to nodes that arrive after the initial click while still loading.
    LaunchedEffect(
        nodeSelectionState.selectAllAwaitingMoreItems,
        uiState.selectableNodeIds,
        uiState.nodesLoadingState,
    ) {
        if (nodeSelectionState.selectAllAwaitingMoreItems) {
            nodeSelectionState.selectAll(uiState.selectableNodeIds, uiState.nodesLoadingState)
        }
    }

    BackHandler(enabled = showSearch) { onCloseSearch() }

    rememberNewFolderResult(
        monitorResult = monitorResult,
        clearResult = clearResult,
        onFolderCreated = { folderId ->
            onNavigate(
                NodesExplorerNavKey(
                    nodeId = folderId,
                    nodeSourceType = nodeSourceType,
                    explorerMode = explorerMode,
                    startNavKey = startNavKey,
                    shareUris = shareUris,
                    disabledNodeIds = disabledNodeIds.toList(),
                )
            )
        },
    )

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .testTag(CLOUD_EXPLORER_VIEW_TAG)
            .fillMaxSize()
            .imePadding()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            when {
                showSearch -> SearchTopAppBar(
                    searchText = searchText,
                    placeholderText = when {
                        isInnerNavigation ->
                            stringResource(
                                sharedR.string.search_placeholder_folder,
                                uiState.folderName.text
                            )

                        selectedTabIndex == INCOMING_TAB_INDEX ->
                            stringResource(sharedR.string.search_placeholder_incoming_shares)

                        selectedTabIndex == FAVOURITES_TAB_INDEX ->
                            stringResource(sharedR.string.search_placeholder_favourites)

                        selectedTabIndex == CHAT_TAB_INDEX ->
                            stringResource(sharedR.string.search_placeholder_chat)

                        else -> stringResource(sharedR.string.search_placeholder_cloud_drive)
                    },
                    onSearchTextChanged = onSearchQueryChanged,
                    onBack = onCloseSearch,
                )

                else -> MegaTopAppBar(
                    navigationType = when {
                        selectedItemsCount > 0 ->
                            AppBarNavigationType.Close { protectedUserTap { clearSelection() } }

                        isInnerNavigation ->
                            AppBarNavigationType.Back { protectedUserTap { onNavigateBack() } }

                        else -> AppBarNavigationType.Close {
                            protectedUserTap {
                                Analytics.tracker.trackEvent(CloudExplorerCloseButtonPressedEvent)
                                onNavigateBack()
                            }
                        }
                    },
                    title = when {
                        uiState.isLoading -> stringResource(explorerMode.titleStringId)
                        nodeSelectionState.selectAllAwaitingMoreItems ->
                            stringResource(sharedR.string.app_bar_selection_mode_description)

                        selectedItemsCount > 0 -> selectedItemsCount.toString()
                        isInnerNavigation -> uiState.folderName.text
                        else -> stringResource(explorerMode.titleStringId)
                    },
                    actions = buildList {
                        if (uiState.hasContent && selectedItemsCount == 0) {
                            add(
                                MenuActionWithClick(CommonMenuAction.Search) {
                                    if (!isProcessingAction) {
                                        if (uiState.isConnected) {
                                            Analytics.tracker.trackEvent(
                                                CloudExplorerSearchButtonPressedEvent
                                            )
                                            showSearch = true
                                        } else {
                                            showNoConnectionSnackbar()
                                        }
                                    }
                                }
                            )
                        }
                        if (!explorerMode.isFolderPicker) {
                            when {
                                nodeSelectionState.selectAllAwaitingMoreItems ->
                                    add(MenuActionWithClick(CommonMenuAction.Selecting) {})

                                uiState.selectableNodeIds.isNotEmpty() && !isAllSelected ->
                                    add(
                                        MenuActionWithClick(CommonMenuAction.SelectAll) {
                                            protectedUserTap {
                                                nodeSelectionState.selectAll(
                                                    uiState.selectableNodeIds,
                                                    uiState.nodesLoadingState,
                                                )
                                            }
                                        }
                                    )
                            }
                        }
                        if (selectedTabIndex == CLOUD_TAB_INDEX && explorerMode.isFolderPicker) {
                            add(
                                MenuActionWithClick(NewFolderMenuAction) {
                                    if (!isProcessingAction) {
                                        if (uiState.isConnected) {
                                            onNavigate(
                                                NewFolderDialogNavKey(
                                                    parentNodeId = nodeExplorerId
                                                )
                                            )
                                        } else {
                                            showNoConnectionSnackbar()
                                        }
                                    }
                                }
                            )
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (!uiState.isLoading && !isProcessingAction && (!showSearch || hasSelection)) {
                InlineAnchoredButtonGroup(
                    modifier = Modifier.testTag(ACTION_BUTTONS_VIEW_TAG),
                    primaryButtonText = stringResource(explorerMode.actionStringId),
                    onPrimaryButtonClick = {
                        protectedUserTap {
                            if (!viewModel.uiState.value.isConnected) {
                                showNoConnectionSnackbar()
                                return@protectedUserTap
                            }
                            if (showSearch) {
                                Analytics.tracker.trackEvent(
                                    CloudExplorerConfirmedSearchButtonPressedEvent
                                )
                            } else {
                                Analytics.tracker.trackEvent(
                                    confirmedTabEvent(
                                        selectedTabIndex,
                                        nodeSourceType
                                    )
                                )
                            }
                            when {
                                explorerMode.isFolderPicker && selectedTabIndex == CHAT_TAB_INDEX ->
                                    onChatsSelected()

                                explorerMode.isFolderPicker ->
                                    onFolderPicked(nodeExplorerId)

                                else ->
                                    onFilesPicked(nodeSelectionState.selectedNodeIds.toList())
                            }
                        }
                    },
                    primaryButtonEnabled = when {
                        !explorerMode.isFolderPicker -> nodeSelectionState.isInSelectionMode
                        selectedTabIndex == CLOUD_TAB_INDEX -> pickerRestrictions?.isPickEnabled
                            ?: (nodeExplorerId != disabledTargetId)

                        selectedTabIndex == CHAT_TAB_INDEX -> chatExplorerSelectionState.isInSelectionMode
                        else -> false
                    },
                    textOnlyButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
                    onTextOnlyButtonClick = {
                        protectedUserTap {
                            Analytics.tracker.trackEvent(CloudExplorerCancelButtonPressedEvent)
                            onCloseExplorerScreen()
                        }
                    },
                )
            }
        }
    ) { paddingValues ->
        MegaCollapsibleTabRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            beyondViewportPageCount = if (showSearch) 0 else 1,
            hideTabs = isInnerNavigation || showSearch,
            pagerScrollEnabled = !showSearch,
            cells = {
                CloudExplorerTab(
                    explorerMode = explorerMode,
                    startNavKey = startNavKey,
                    nodeExplorerId = nodeExplorerId,
                    nodeSourceType = nodeSourceType,
                    shareUris = shareUris,
                    showSearch = showSearch,
                    searchQuery = searchText,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onCloseSearch = onCloseSearch,
                    protectedUserTap = protectedUserTap,
                    onNavigate = onConnectedNavigate,
                    onNavigateBack = onNavigateBack,
                    selectionState = nodeSelectionState,
                    isFileSelectionEnabled = !explorerMode.isFolderPicker,
                    disabledNodeIds = disabledNodeIds,
                    videosOnly = explorerMode.isVideoPicker,
                    restrictedNodeIds = pickerRestrictions?.restrictedNodeIds.orEmpty(),
                    onRestrictedNodeClick = {
                        pickerRestrictions?.onRestrictedNodeClick?.invoke(it)
                    },
                )
                if (!isInnerNavigation && explorerMode.isIncomingAvailable) {
                    IncomingExplorerTab(
                        explorerMode = explorerMode,
                        startNavKey = startNavKey,
                        shareUris = shareUris,
                        showSearch = showSearch,
                        searchQuery = searchText,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onCloseSearch = onCloseSearch,
                        protectedUserTap = protectedUserTap,
                        onNavigate = onConnectedNavigate,
                        onNavigateBack = onNavigateBack,
                    )
                }
                if (!isInnerNavigation) {
                    FavouritesExplorerTab(
                        explorerMode = explorerMode,
                        startNavKey = startNavKey,
                        shareUris = shareUris,
                        showSearch = showSearch,
                        searchQuery = searchText,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onCloseSearch = onCloseSearch,
                        protectedUserTap = protectedUserTap,
                        onNavigate = onConnectedNavigate,
                        onNavigateBack = onNavigateBack,
                        selectionState = nodeSelectionState,
                        isSelectionModeEnabled = !explorerMode.isFolderPicker,
                        disabledNodeIds = disabledNodeIds,
                        videosOnly = explorerMode.isVideoPicker,
                    )
                }
                if (!isInnerNavigation && explorerMode.isChatAvailable) {
                    ChatExplorerTab(
                        shareTextToMegaNavKey = startNavKey as? ShareTextToMegaNavKey,
                        selectionState = chatExplorerSelectionState,
                        isProcessingAction = isProcessingAction,
                        showSearch = showSearch,
                        searchQuery = searchText,
                        onSearchQueryChanged = onSearchQueryChanged,
                        prepareChatsEvent = prepareChatsEvent,
                        onPrepareChatsConsumed = onPrepareChatsConsumed,
                        onChatsReadyToShare = onChatsReadyToShare,
                        onCloseExplorerScreen = onCloseExplorerScreen,
                        onNavigate = onConnectedNavigate,
                        monitorResult = monitorResult,
                        clearResult = clearResult,
                    )
                }
            },
            initialSelectedIndex = selectedTabIndex,
            onTabSelected = {
                selectedTabIndex = it
                true
            }
        )
    }
}

internal fun confirmedTabEvent(
    selectedTabIndex: Int,
    nodeSourceType: NodeSourceType,
): EventIdentifier = when {
    selectedTabIndex == CHAT_TAB_INDEX -> CloudExplorerConfirmedChatButtonPressedEvent
    selectedTabIndex == INCOMING_TAB_INDEX || nodeSourceType == NodeSourceType.INCOMING_SHARES ->
        CloudExplorerConfirmedIncomingButtonPressedEvent

    selectedTabIndex == FAVOURITES_TAB_INDEX || nodeSourceType == NodeSourceType.FAVOURITES ->
        CloudExplorerConfirmedFavouritesButtonPressedEvent

    else -> CloudExplorerConfirmedCloudButtonPressedEvent
}

internal fun navigateToFolder(
    nodeSourceType: NodeSourceType,
    explorerMode: ExplorerMode,
    startNavKey: ExplorerNavKey,
    shareUris: List<UriPath>?,
    disabledNodeIds: List<NodeId> = emptyList(),
    protectedUserTap: (() -> Unit) -> Unit,
    onNavigate: (NavKey) -> Unit,
): (NodeId) -> Unit = { nodeId ->
    protectedUserTap {
        onNavigate(
            NodesExplorerNavKey(
                nodeId = nodeId,
                nodeSourceType = nodeSourceType,
                explorerMode = explorerMode,
                startNavKey = startNavKey,
                shareUris = shareUris,
                disabledNodeIds = disabledNodeIds,
            )
        )
    }
}

internal fun navigateToFolderPath(
    nodeSourceType: NodeSourceType,
    explorerMode: ExplorerMode,
    startNavKey: ExplorerNavKey,
    shareUris: List<UriPath>?,
    disabledNodeIds: List<NodeId> = emptyList(),
    protectedUserTap: (() -> Unit) -> Unit,
    onNavigate: (NavKey) -> Unit,
): (List<NodeId>) -> Unit {
    val navigateToFolder = navigateToFolder(
        nodeSourceType = nodeSourceType,
        explorerMode = explorerMode,
        startNavKey = startNavKey,
        shareUris = shareUris,
        disabledNodeIds = disabledNodeIds,
        protectedUserTap = protectedUserTap,
        onNavigate = onNavigate,
    )
    return { nodeIds -> nodeIds.forEach(navigateToFolder) }
}

/**
 * The items to render after applying the hidden-nodes filter: sensitive nodes are dropped unless
 * the user opted to show them or the hidden-nodes feature is off. Shared by every explorer source so
 * the filtering rule stays in one place.
 */
@Composable
internal fun rememberVisibleItems(
    items: List<NodeViewItem<TypedNode>>,
    showHiddenNodes: Boolean,
    isHiddenNodesEnabled: Boolean,
): List<NodeViewItem<TypedNode>> =
    remember(showHiddenNodes, isHiddenNodesEnabled, items) {
        visibleNodeItems(items, showHiddenNodes, isHiddenNodesEnabled)
    }

internal const val CLOUD_EXPLORER_VIEW_TAG = "cloud_explorer_view"
internal const val ACTION_BUTTONS_VIEW_TAG = "$CLOUD_EXPLORER_VIEW_TAG:action_buttons"
internal const val CLOUD_TAB_TAG = "$CLOUD_EXPLORER_VIEW_TAG:cloud_tab"
internal const val INCOMING_TAB_TAG = "$CLOUD_EXPLORER_VIEW_TAG:incoming_tab"
internal const val FAVOURITES_TAB_TAG = "$CLOUD_EXPLORER_VIEW_TAG:favourites_tab"
internal const val CHAT_TAB_TAG = "$CLOUD_EXPLORER_VIEW_TAG:chat_tab"
internal const val CLOUD_TAB_INDEX = 0
internal const val INCOMING_TAB_INDEX = 1
internal const val FAVOURITES_TAB_INDEX = 2
internal const val CHAT_TAB_INDEX = 3
