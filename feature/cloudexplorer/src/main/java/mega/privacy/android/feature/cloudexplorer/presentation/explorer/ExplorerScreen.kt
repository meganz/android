package mega.privacy.android.feature.cloudexplorer.presentation.explorer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.ChatExplorerTab
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.rememberChatExplorerSelectionState
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.extensions.actionStringId
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.extensions.titleStringId
import mega.privacy.android.feature.cloudexplorer.presentation.favouritesexplorer.FavouritesExplorerTab
import mega.privacy.android.feature.cloudexplorer.presentation.incomingsharesexplorer.IncomingExplorerTab
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerSharedViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerScreenContent
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.search.NodesExplorerSearchContent
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.navigation.contract.menu.NewFolderMenuAction
import mega.privacy.android.navigation.destination.ExplorerNavKey
import mega.privacy.android.navigation.destination.NodesExplorerNavKey
import mega.privacy.android.navigation.destination.ShareTextToMegaNavKey
import mega.privacy.android.shared.nodes.dialog.newfolder.NewFolderNodeDialog
import mega.privacy.android.shared.nodes.selection.rememberNodeSelectionState
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.search.presentation.component.SearchTopAppBar

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
    var showNewFolderDialog by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    // Raw search-field text is host UI state; each tab owns its own search ViewModel + content.
    var searchText by rememberSaveable { mutableStateOf("") }
    val onSearchQueryChanged: (String) -> Unit = { searchText = it }
    val onCloseSearch: () -> Unit = {
        searchText = ""
        showSearch = false
    }
    val protectedUserTap: (() -> Unit) -> Unit = { action -> if (!isProcessingAction) action() }


    BackHandler(enabled = showSearch) { onCloseSearch() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackBarHostState.current
    val resources = LocalResources.current
    val viewModel =
        hiltViewModel<NodesExplorerViewModel, NodesExplorerViewModel.Factory> { factory ->
            factory.create(
                args = NodeExplorerSharedViewModel.Args(
                    nodeExplorerId,
                    nodeSourceType,
                )
            )
        }
    val uiState by viewModel.nodesExplorerUiState.collectAsStateWithLifecycle()
    val uiStateShared by viewModel.nodeExplorerSharedUiState.collectAsStateWithLifecycle()
    val chatExplorerSelectionState = rememberChatExplorerSelectionState()
    val nodeSelectionState = rememberNodeSelectionState()
    val isFileSelectionEnabled = !explorerMode.isFolderPicker
    val videosOnly = explorerMode.isVideoPicker
    val tabHasContent = remember { mutableStateMapOf<Int, Boolean>() }
    val currentListHasContent = when {
        isInnerNavigation || selectedTabIndex == CLOUD_TAB_INDEX -> uiStateShared.items.isNotEmpty()
        else -> tabHasContent[selectedTabIndex] == true
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .testTag(CLOUD_EXPLORER_VIEW_TAG)
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            if (showSearch) {
                SearchTopAppBar(
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
            } else {
                MegaTopAppBar(
                    navigationType = if (isInnerNavigation) {
                        AppBarNavigationType.Back { protectedUserTap { onNavigateBack() } }
                    } else {
                        AppBarNavigationType.Close { protectedUserTap { onNavigateBack() } }
                    },
                    title = when {
                        chatExplorerSelectionState.selectedItemsCount > 0 -> chatExplorerSelectionState.selectedItemsCount.toString()
                        isInnerNavigation -> uiState.folderName.text
                        else -> stringResource(explorerMode.titleStringId)
                    },
                    actions = buildList {
                        if (selectedTabIndex == CLOUD_TAB_INDEX && explorerMode.isFolderPicker) {
                            add(
                                MenuActionWithClick(NewFolderMenuAction) {
                                    if (!isProcessingAction) {
                                        showNewFolderDialog = true
                                    }
                                }
                            )
                        }
                        if (currentListHasContent) {
                            add(
                                MenuActionWithClick(CommonMenuAction.Search) {
                                    if (!isProcessingAction) {
                                        showSearch = true
                                    }
                                }
                            )
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (!isProcessingAction && !showSearch) {
                InlineAnchoredButtonGroup(
                    modifier = Modifier.testTag(ACTION_BUTTONS_VIEW_TAG),
                    primaryButtonText = stringResource(explorerMode.actionStringId),
                    onPrimaryButtonClick = {
                        protectedUserTap {
                            when {
                                explorerMode.isFolderPicker && selectedTabIndex == CHAT_TAB_INDEX ->
                                    onChatsSelected()

                                explorerMode.isFolderPicker ->
                                    onFolderPicked(uiStateShared.currentFolderId)

                                else ->
                                    onFilesPicked(nodeSelectionState.selectedNodeIds.toList())
                            }
                        }
                    },
                    primaryButtonEnabled = when {
                        !explorerMode.isFolderPicker -> nodeSelectionState.isInSelectionMode
                        selectedTabIndex == CLOUD_TAB_INDEX -> uiStateShared.currentFolderId != disabledTargetId
                        selectedTabIndex == CHAT_TAB_INDEX -> chatExplorerSelectionState.isInSelectionMode
                        else -> false
                    },
                    textOnlyButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
                    onTextOnlyButtonClick = { protectedUserTap { onCloseExplorerScreen() } },
                )
            }
        }
    ) { paddingValues ->
        val onFolderClick: (NodeId) -> Unit = { nodeId ->
            protectedUserTap {
                onNavigate(
                    NodesExplorerNavKey(
                        nodeId = nodeId,
                        nodeSourceType = uiStateShared.nodeSourceType,
                        explorerMode = explorerMode,
                        startNavKey = startNavKey,
                        shareUris = shareUris,
                        disabledNodeIds = disabledNodeIds.toList(),
                    )
                )
            }
        }

        MegaCollapsibleTabRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            beyondViewportPageCount = 1,
            hideTabs = isInnerNavigation || showSearch,
            cells = {
                addTextTabWithScrollableContent(
                    tabItem = TabItems(
                        title = stringResource(sharedR.string.general_section_cloud_drive),
                        testTag = CLOUD_TAB_TAG
                    ),
                ) { _, modifier ->
                    if (showSearch) {
                        NodesExplorerSearchContent(
                            query = searchText,
                            onQueryChanged = onSearchQueryChanged,
                            nodeSelectionState = nodeSelectionState,
                            isFileSelectionEnabled = isFileSelectionEnabled,
                            videosOnly = videosOnly,
                            disabledNodeIds = disabledNodeIds,
                            onNavigateToFolderPath = navigateToFolderPath(
                                nodeSourceType = uiStateShared.nodeSourceType,
                                explorerMode = explorerMode,
                                startNavKey = startNavKey,
                                shareUris = shareUris,
                                disabledNodeIds = disabledNodeIds.toList(),
                                protectedUserTap = protectedUserTap,
                                onNavigate = onNavigate,
                            ),
                            onCloseSearch = onCloseSearch,
                            modifier = modifier,
                        )
                    } else {
                        NodesExplorerScreenContent(
                            uiState = uiState,
                            uiStateShared = uiStateShared,
                            onNavigateBack = { protectedUserTap { onNavigateBack() } },
                            consumeNavigateBack = viewModel::onNavigateBackEventConsumed,
                            onFolderClick = onFolderClick,
                            onRefreshNodes = viewModel::refreshNodes,
                            selectionState = nodeSelectionState,
                            isSelectionModeEnabled = isFileSelectionEnabled,
                            disabledNodeIds = disabledNodeIds,
                            videosOnly = videosOnly,
                            modifier = modifier,
                        )
                    }
                }
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
                        onNavigate = onNavigate,
                        onNavigateBack = onNavigateBack,
                        onHasContentChanged = { tabHasContent[INCOMING_TAB_INDEX] = it },
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
                        onNavigate = onNavigate,
                        onNavigateBack = onNavigateBack,
                        selectionState = nodeSelectionState,
                        isSelectionModeEnabled = isFileSelectionEnabled,
                        disabledNodeIds = disabledNodeIds,
                        videosOnly = videosOnly,
                        onHasContentChanged = { tabHasContent[FAVOURITES_TAB_INDEX] = it },
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
                        onNavigate = onNavigate,
                        monitorResult = monitorResult,
                        clearResult = clearResult,
                        onHasContentChanged = { tabHasContent[CHAT_TAB_INDEX] = it },
                    )
                }
            },
            initialSelectedIndex = selectedTabIndex,
            onTabSelected = {
                selectedTabIndex = it
                true
            }
        )

        if (showNewFolderDialog) {
            NewFolderNodeDialog(
                parentNode = uiStateShared.currentFolderId,
                onCreateFolder = { folderId ->
                    showNewFolderDialog = false
                    coroutineScope.launch {
                        folderId?.let {
                            onNavigate(
                                NodesExplorerNavKey(
                                    nodeId = it,
                                    nodeSourceType = uiStateShared.nodeSourceType,
                                    explorerMode = explorerMode,
                                    startNavKey = startNavKey,
                                    shareUris = shareUris,
                                    disabledNodeIds = disabledNodeIds.toList(),
                                )
                            )
                        } ?: snackbarHostState?.showAutoDurationSnackbar(
                            resources.getString(
                                sharedR.string.folder_not_created_error_message
                            )
                        )
                    }
                },
                onDismiss = {
                    showNewFolderDialog = false
                }
            )
        }
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
): (List<NodeId>) -> Unit = { nodeIds ->
    protectedUserTap {
        nodeIds.forEach { nodeId ->
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
