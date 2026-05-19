package mega.privacy.android.feature.cloudexplorer.presentation.explorer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
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
import mega.privacy.android.navigation.contract.menu.NewFolderMenuAction
import mega.privacy.android.navigation.destination.CreateGroupChatNavKey
import mega.privacy.android.navigation.destination.ExplorerNavKey
import mega.privacy.android.navigation.destination.NodesExplorerNavKey
import mega.privacy.android.shared.nodes.dialog.newfolder.NewFolderNodeDialog
import mega.privacy.android.shared.resources.R as sharedR

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
    onFolderPicked: (NodeId) -> Unit = {},
    onFilesPicked: (List<NodeId>) -> Unit = {},
    onChatsSelected: (List<Long>) -> Unit = {},
    onStartNewGroupChat: ((CreateGroupChatNavKey.NewGroupChatResult) -> Unit) -> Unit = {},
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(tabIndex) }
    var showNewFolderDialog by rememberSaveable { mutableStateOf(false) }
    val protectedUserTap: (() -> Unit) -> Unit = { action -> if (!isProcessingAction) action() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackBarHostState.current
    val resources = LocalResources.current
    val nodesExplorerViewModel =
        hiltViewModel<NodesExplorerViewModel, NodesExplorerViewModel.Factory> { factory ->
            factory.create(
                args = NodeExplorerSharedViewModel.Args(
                    nodeExplorerId,
                    nodeSourceType,
                )
            )
        }
    val nodesExplorerUiState by nodesExplorerViewModel.nodesExplorerUiState.collectAsStateWithLifecycle()
    val nodesExplorerUiStateShared by nodesExplorerViewModel.nodeExplorerSharedUiState.collectAsStateWithLifecycle()
    val chatExplorerSelectionState = rememberChatExplorerSelectionState()

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .testTag(CLOUD_EXPLORER_VIEW_TAG)
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            MegaTopAppBar(
                navigationType = if (isInnerNavigation) {
                    AppBarNavigationType.Back { protectedUserTap { onNavigateBack() } }
                } else {
                    AppBarNavigationType.Close { protectedUserTap { onNavigateBack() } }
                },
                title = if (isInnerNavigation) {
                    nodesExplorerUiState.folderName.text
                } else {
                    stringResource(explorerMode.titleStringId)
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
                    //TODO Add search and select all per tab (cloud-style tabs use isFolderPicker().not() for select all)
                },
            )
        },
        bottomBar = {
            InlineAnchoredButtonGroup(
                modifier = Modifier.testTag(ACTION_BUTTONS_VIEW_TAG),
                primaryButtonText = stringResource(explorerMode.actionStringId),
                onPrimaryButtonClick = {
                    protectedUserTap {
                        when {
                            explorerMode.isFolderPicker && selectedTabIndex == CHAT_TAB_INDEX ->
                                onChatsSelected(chatExplorerSelectionState.selectedChatIds.toList())

                            explorerMode.isFolderPicker ->
                                onFolderPicked(nodesExplorerUiStateShared.currentFolderId)

                            else ->
                                //Replace with valid nodeIds list
                                onFilesPicked(emptyList())
                        }
                    }
                },
                primaryButtonEnabled = when {
                    !explorerMode.isFolderPicker -> true
                    selectedTabIndex == CLOUD_TAB_INDEX -> true
                    selectedTabIndex == CHAT_TAB_INDEX -> chatExplorerSelectionState.isInSelectionMode
                    else -> false
                },
                textOnlyButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
                onTextOnlyButtonClick = { protectedUserTap { onCloseExplorerScreen() } },
            )
        }
    ) { paddingValues ->
        MegaCollapsibleTabRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            beyondViewportPageCount = 1,
            hideTabs = isInnerNavigation,
            cells = {
                addTextTabWithScrollableContent(
                    tabItem = TabItems(
                        title = stringResource(sharedR.string.general_section_cloud_drive),
                        testTag = CLOUD_TAB_TAG
                    ),
                ) { _, modifier ->
                    NodesExplorerScreenContent(
                        uiState = nodesExplorerUiState,
                        uiStateShared = nodesExplorerUiStateShared,
                        onNavigateBack = { protectedUserTap { onNavigateBack() } },
                        consumeNavigateBack = nodesExplorerViewModel::onNavigateBackEventConsumed,
                        onFolderClick = { nodeId ->
                            protectedUserTap {
                                onNavigate(
                                    NodesExplorerNavKey(
                                        nodeId = nodeId,
                                        nodeSourceType = nodesExplorerUiStateShared.nodeSourceType,
                                        explorerMode = explorerMode,
                                        startNavKey = startNavKey,
                                        shareUris = shareUris,
                                    )
                                )
                            }
                        },
                        onRefreshNodes = nodesExplorerViewModel::refreshNodes,
                        modifier = modifier,
                    )
                }
                if (!isInnerNavigation && explorerMode.isIncomingAvailable) {
                    IncomingExplorerTab(
                        explorerMode = explorerMode,
                        startNavKey = startNavKey,
                        shareUris = shareUris,
                        protectedUserTap = protectedUserTap,
                        onNavigate = onNavigate,
                        onNavigateBack = onNavigateBack,
                    )
                }
                if (!isInnerNavigation) {
                    FavouritesExplorerTab(
                        explorerMode = explorerMode,
                        startNavKey = startNavKey,
                        shareUris = shareUris,
                        protectedUserTap = protectedUserTap,
                        onNavigate = onNavigate,
                        onNavigateBack = onNavigateBack,
                    )
                }
                if (!isInnerNavigation && explorerMode.isChatAvailable) {
                    ChatExplorerTab(
                        selectionState = chatExplorerSelectionState,
                        onStartNewGroupChat = onStartNewGroupChat,
                    )
                }
            },
            initialSelectedIndex = tabIndex,
            onTabSelected = {
                selectedTabIndex = it
                true
            }
        )

        if (showNewFolderDialog) {
            NewFolderNodeDialog(
                parentNode = nodesExplorerUiStateShared.currentFolderId,
                onCreateFolder = { folderId ->
                    showNewFolderDialog = false
                    coroutineScope.launch {
                        folderId?.let {
                            onNavigate(
                                NodesExplorerNavKey(
                                    nodeId = it,
                                    nodeSourceType = nodesExplorerUiStateShared.nodeSourceType,
                                    explorerMode = explorerMode,
                                    startNavKey = startNavKey,
                                    shareUris = shareUris,
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
