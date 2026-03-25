package mega.privacy.android.feature.cloudexplorer.presentation.explorer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.button.InlineAnchoredButtonGroup
import mega.android.core.ui.components.tabs.MegaCollapsibleTabRow
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.TabItems
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.model.ExplorerModeData
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.model.toMode
import mega.privacy.android.feature.cloudexplorer.presentation.favouritesexplorer.FavouritesExplorerContent
import mega.privacy.android.feature.cloudexplorer.presentation.favouritesexplorer.FavouritesExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.incomingsharesexplorer.IncomingSharesExplorerContent
import mega.privacy.android.feature.cloudexplorer.presentation.incomingsharesexplorer.IncomingSharesExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerSharedViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerScreenContent
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerViewModel
import mega.privacy.android.navigation.destination.NodesExplorerNavKey
import mega.privacy.android.shared.resources.R as sharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    explorerModeData: ExplorerModeData,
    isInnerNavigation: Boolean,
    nodeExplorerId: NodeId,
    nodeSourceType: NodeSourceType,
    onNavigateBack: () -> Unit,
    onNavigateToFolder: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    tabIndex: Int = CLOUD_TAB_INDEX,
    onFolderPicked: (NodeId) -> Unit = {},
    onFilesPicked: (List<NodeId>) -> Unit = {},
    onChatsSelected: (List<Long>) -> Unit = {},
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(tabIndex) }
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
    val incomingSharesExplorerViewModel =
        if (!isInnerNavigation && explorerModeData.isIncomingAvailable)
        hiltViewModel<IncomingSharesExplorerViewModel>() else null
    val incomingSharesExplorerUiStateShared =
        incomingSharesExplorerViewModel?.nodeExplorerSharedUiState?.collectAsStateWithLifecycle()
    val favouritesExplorerViewModel =
        if (!isInnerNavigation) hiltViewModel<FavouritesExplorerViewModel, FavouritesExplorerViewModel.Factory> { factory ->
            factory.create(
                args = FavouritesExplorerViewModel.Args(showFiles = !explorerModeData.isFolderPicker)
            )
        } else null
    val favouritesExplorerUiStateShared =
        favouritesExplorerViewModel?.nodeExplorerSharedUiState?.collectAsStateWithLifecycle()

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .testTag(CLOUD_EXPLORER_VIEW_TAG)
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            MegaTopAppBar(
                navigationType = if (isInnerNavigation) {
                    AppBarNavigationType.Back { onNavigateBack() }
                } else {
                    AppBarNavigationType.Close { onNavigateBack() }
                },
                title = if (isInnerNavigation) {
                    nodesExplorerUiState.folderName.text
                } else {
                    stringResource(explorerModeData.titleStringId)
                },
                actions = buildList {
                    when (selectedTabIndex) {
                        CLOUD_TAB_INDEX -> {
                            if (nodesExplorerUiStateShared.items.isNotEmpty()) {
                                //Add search
                                //Add new folder in case explorerMode.isFolderPicker()
                                //Add select all in case explorerMode.isFolderPicker().not()
                            }
                        }

                        INCOMING_TAB_INDEX -> {
                            //Add search if not empty
                        }

                        FAVOURITES_TAB_INDEX -> {
                            //Add search if not empty
                        }

                        CHAT_TAB_INDEX -> {
                            //Add search, never empty as at least the note to self is available
                        }
                    }
                },
            )
        },
        bottomBar = {
            InlineAnchoredButtonGroup(
                modifier = Modifier.testTag(ACTION_BUTTONS_VIEW_TAG),
                primaryButtonText = stringResource(explorerModeData.actionStringId),
                onPrimaryButtonClick = {
                    when {
                        explorerModeData.isFolderPicker && selectedTabIndex != CHAT_TAB_INDEX -> {
                            onFolderPicked(nodesExplorerUiStateShared.currentFolderId)
                        }

                        explorerModeData.isFolderPicker && selectedTabIndex == CHAT_TAB_INDEX -> {
                            //Replace with valid chatId list
                            onChatsSelected(emptyList())
                        }

                        else -> {
                            //Replace with valid nodeIds list
                            onFilesPicked(emptyList())
                        }
                    }
                },
                textOnlyButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
                onTextOnlyButtonClick = onNavigateBack,
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
                        onNavigateBack = onNavigateBack,
                        consumeNavigateBack = nodesExplorerViewModel::onNavigateBackEventConsumed,
                        onFolderClick = {
                            onNavigateToFolder(
                                NodesExplorerNavKey(
                                    nodeId = it,
                                    nodeSourceType = nodesExplorerUiStateShared.nodeSourceType,
                                    explorerMode = explorerModeData.toMode()
                                )
                            )
                        },
                        onFileClick = {},
                        onRefreshNodes = nodesExplorerViewModel::refreshNodes,
                        modifier = modifier,
                    )
                }
                if (incomingSharesExplorerUiStateShared?.value != null) {
                    addTextTabWithScrollableContent(
                        tabItem = TabItems(
                            title = stringResource(sharedR.string.general_title_incoming_shares),
                            testTag = INCOMING_TAB_TAG
                        ),
                    ) { _, modifier ->
                        IncomingSharesExplorerContent(
                            uiStateShared = incomingSharesExplorerUiStateShared.value,
                            onNavigateBack = onNavigateBack,
                            consumeNavigateBack = incomingSharesExplorerViewModel::onNavigateBackEventConsumed,
                            onFolderClick = {
                                onNavigateToFolder(
                                    NodesExplorerNavKey(
                                        nodeId = it,
                                        nodeSourceType = incomingSharesExplorerUiStateShared.value.nodeSourceType,
                                        explorerMode = explorerModeData.toMode()
                                    )
                                )
                            },
                            onFileClick = {},
                            onRefreshNodes = incomingSharesExplorerViewModel::refreshNodes,
                            modifier = modifier
                        )
                    }
                }
                if (favouritesExplorerUiStateShared?.value != null) {
                    addTextTabWithScrollableContent(
                        tabItem = TabItems(
                            title = stringResource(sharedR.string.video_section_title_favourite_playlist),
                            testTag = FAVOURITES_TAB_TAG
                        ),
                    ) { _, modifier ->
                        FavouritesExplorerContent(
                            uiStateShared = favouritesExplorerUiStateShared.value,
                            onNavigateBack = onNavigateBack,
                            consumeNavigateBack = favouritesExplorerViewModel::onNavigateBackEventConsumed,
                            onFolderClick = {
                                onNavigateToFolder(
                                    NodesExplorerNavKey(
                                        nodeId = it,
                                        nodeSourceType = favouritesExplorerUiStateShared.value.nodeSourceType,
                                        explorerMode = explorerModeData.toMode()
                                    )
                                )
                            },
                            onFileClick = favouritesExplorerViewModel::fileClicked,
                            onRefreshNodes = favouritesExplorerViewModel::refreshNodes,
                            modifier = modifier
                        )
                    }
                }
                if (explorerModeData.isChatAvailable) {
                    //Add chat tab
                }
            },
            initialSelectedIndex = tabIndex,
            onTabSelected = {
                selectedTabIndex = it
                true
            }
        )
    }
}

internal const val CLOUD_EXPLORER_VIEW_TAG = "cloud_explorer_view"
internal const val ACTION_BUTTONS_VIEW_TAG = "$CLOUD_EXPLORER_VIEW_TAG:action_buttons"
internal const val CLOUD_TAB_TAG = "$CLOUD_EXPLORER_VIEW_TAG:cloud_tab"
internal const val INCOMING_TAB_TAG = "$CLOUD_EXPLORER_VIEW_TAG:incoming_tab"
internal const val FAVOURITES_TAB_TAG = "$CLOUD_EXPLORER_VIEW_TAG:favourites_tab"
internal const val CLOUD_TAB_INDEX = 0
internal const val INCOMING_TAB_INDEX = 1
internal const val FAVOURITES_TAB_INDEX = 2
internal const val CHAT_TAB_INDEX = 3