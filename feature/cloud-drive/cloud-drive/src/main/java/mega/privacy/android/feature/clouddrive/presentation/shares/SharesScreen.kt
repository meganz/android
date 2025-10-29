package mega.privacy.android.feature.clouddrive.presentation.shares

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.components.tabs.MegaScrollableTabRow
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeAppBar
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.dialog.rename.RenameNodeDialogNavKey
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderAccessDialogNavKey
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderDialogM3
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetRoute
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheet
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheetResult
import mega.privacy.android.core.sharedcomponents.extension.excludingBottomPadding
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.feature.clouddrive.model.CloudDriveAppBarAction
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.HandleNodeOptionEvent
import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.IncomingSharesContent
import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.IncomingSharesViewModel
import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.model.IncomingSharesAction
import mega.privacy.android.feature.clouddrive.presentation.shares.links.LinksContent
import mega.privacy.android.feature.clouddrive.presentation.shares.links.LinksViewModel
import mega.privacy.android.feature.clouddrive.presentation.shares.links.model.LinksAction
import mega.privacy.android.feature.clouddrive.presentation.shares.outgoingshares.OutgoingSharesContent
import mega.privacy.android.feature.clouddrive.presentation.shares.outgoingshares.OutgoingSharesViewModel
import mega.privacy.android.feature.clouddrive.presentation.shares.outgoingshares.model.OutgoingSharesAction
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.SearchNodeNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.navigation.extensions.rememberMegaResultContract
import mega.privacy.android.shared.resources.R as sharedR


/**
 * Shares screen containing incoming shares, outgoing shares and links tabs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SharesScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
    nodeOptionsActionViewModel: NodeOptionsActionViewModel = hiltViewModel(),
    incomingSharesViewModel: IncomingSharesViewModel = hiltViewModel(),
    outgoingSharesViewModel: OutgoingSharesViewModel = hiltViewModel(),
    linksViewModel: LinksViewModel = hiltViewModel(),
) {
    val megaNavigator = rememberMegaNavigator()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackBarHostState.current
    var selectedTab by rememberSaveable { mutableStateOf(SharesTab.IncomingShares) }
    val nodeActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val nodeActionHandler = rememberNodeActionHandler(
        navigationHandler = navigationHandler,
        viewModel = nodeOptionsActionViewModel,
        megaNavigator = megaNavigator
    )
    val incomingSharesUiState by incomingSharesViewModel.uiState.collectAsStateWithLifecycle()
    val outgoingSharesUiState by outgoingSharesViewModel.uiState.collectAsStateWithLifecycle()
    val linksUiState by linksViewModel.uiState.collectAsStateWithLifecycle()

    // Sort modal
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }

    // Node options modal state
    var visibleNodeOptionId by remember { mutableStateOf<NodeId?>(null) }
    val nodeOptionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var shareNodeHandles by remember { mutableStateOf<List<Long>>(emptyList()) }
    val megaResultContract = rememberMegaResultContract()
    val shareFolderLauncher = rememberLauncherForActivityResult(
        contract = megaResultContract.shareFolderActivityResultContract
    ) { result ->
        result?.let { (contactIds, nodeHandles) ->
            nodeOptionsActionViewModel.contactSelectedForShareFolder(
                contactIds,
                nodeHandles
            )
        }
    }
    val nameCollisionLauncher = rememberLauncherForActivityResult(
        contract = megaResultContract.nameCollisionActivityContract
    ) { message ->
        if (!message.isNullOrEmpty()) {
            coroutineScope.launch {
                snackbarHostState?.showAutoDurationSnackbar(message)
            }
        }
    }

    val (isInSelectionMode, selectedItemsCount) = when (selectedTab) {
        SharesTab.IncomingShares -> incomingSharesUiState.isInSelectionMode to incomingSharesUiState.selectedItemsCount
        SharesTab.OutgoingShares -> outgoingSharesUiState.isInSelectionMode to outgoingSharesUiState.selectedItemsCount
        SharesTab.Links -> linksUiState.isInSelectionMode to linksUiState.selectedItemsCount
    }

    fun deselectAllItems() {
        when (selectedTab) {
            SharesTab.IncomingShares -> incomingSharesViewModel.processAction(IncomingSharesAction.DeselectAllItems)
            SharesTab.OutgoingShares -> outgoingSharesViewModel.processAction(OutgoingSharesAction.DeselectAllItems)
            SharesTab.Links -> linksViewModel.processAction(LinksAction.DeselectAllItems)
        }
    }

    fun selectAllItems() {
        when (selectedTab) {
            SharesTab.IncomingShares -> incomingSharesViewModel.processAction(IncomingSharesAction.SelectAllItems)
            SharesTab.OutgoingShares -> outgoingSharesViewModel.processAction(OutgoingSharesAction.SelectAllItems)
            SharesTab.Links -> linksViewModel.processAction(LinksAction.SelectAllItems)

        }
    }

    fun getSelectedNodes() = if (isInSelectionMode) {
        when (selectedTab) {
            SharesTab.IncomingShares -> incomingSharesUiState.selectedNodes
            SharesTab.OutgoingShares -> outgoingSharesUiState.selectedNodes
            SharesTab.Links -> linksUiState.selectedNodes

        }
    } else {
        emptyList()
    }

    BackHandler(enabled = isInSelectionMode) {
        deselectAllItems()
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        topBar = {
            if (isInSelectionMode) {
                NodeSelectionModeAppBar(
                    count = selectedItemsCount,
                    isSelecting = false,
                    onSelectAllClicked = {
                        selectAllItems()
                    },
                    onCancelSelectionClicked = {
                        deselectAllItems()
                    }
                )
            } else {
                MegaTopAppBar(
                    navigationType = AppBarNavigationType.Back {
                        navigationHandler.back()
                    },
                    title = stringResource(R.string.title_shared_items),
                    trailingIcons = {
                        TransfersToolbarWidget(navigationHandler)
                    },
                    actions = buildList {
                        add(
                            MenuActionWithClick(CloudDriveAppBarAction.Search) {
                                navigationHandler.navigate(
                                    SearchNodeNavKey(
                                        isFirstNavigationLevel = false,
                                        nodeSourceType = selectedTab.toNodeSourceType(),
                                        parentHandle = -1L
                                    )
                                )
                            }
                        )
                    },
                )
            }
        },
        bottomBar = {
            NodeSelectionModeBottomBar(
                availableActions = nodeActionState.availableActions,
                visibleActions = nodeActionState.visibleActions,
                visible = nodeActionState.visibleActions.isNotEmpty() && isInSelectionMode,
                nodeActionHandler = nodeActionHandler,
                selectedNodes = getSelectedNodes(),
                isSelecting = false
            )
        },
    ) { paddingValues ->
        MegaScrollableTabRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues.excludingBottomPadding()),
            beyondViewportPageCount = 1,
            hideTabs = isInSelectionMode,
            pagerScrollEnabled = !isInSelectionMode,
            cells = {
                addTextTabWithScrollableContent(
                    tabItem = TabItems(stringResource(R.string.tab_incoming_shares)),
                ) { _, modifier ->
                    IncomingSharesContent(
                        modifier = modifier,
                        uiState = incomingSharesUiState,
                        navigationHandler = navigationHandler,
                        onAction = incomingSharesViewModel::processAction,
                        onShowNodeOptions = { visibleNodeOptionId = it },
                        onSortOrderClick = { showSortBottomSheet = true },
                        contentPadding = PaddingValues(
                            bottom = paddingValues.calculateBottomPadding()
                        ),
                    )
                }
                addTextTabWithScrollableContent(
                    tabItem = TabItems(stringResource(R.string.tab_outgoing_shares)),
                ) { _, modifier ->
                    OutgoingSharesContent(
                        modifier = modifier,
                        uiState = outgoingSharesUiState,
                        navigationHandler = navigationHandler,
                        onAction = outgoingSharesViewModel::processAction,
                        onShowNodeOptions = { visibleNodeOptionId = it },
                        onSortOrderClick = { showSortBottomSheet = true },
                        contentPadding = PaddingValues(
                            bottom = paddingValues.calculateBottomPadding()
                        ),
                    )
                }
                addTextTabWithScrollableContent(
                    tabItem = TabItems(stringResource(R.string.tab_links_shares)),
                ) { _, modifier ->
                    LinksContent(
                        modifier = modifier,
                        uiState = linksUiState,
                        navigationHandler = navigationHandler,
                        onAction = linksViewModel::processAction,
                        onShowNodeOptions = { visibleNodeOptionId = it },
                        onSortOrderClick = { showSortBottomSheet = true },
                        onTransfer = onTransfer,
                        contentPadding = PaddingValues(
                            bottom = paddingValues.calculateBottomPadding()
                        ),
                    )
                }
            },
            initialSelectedIndex = SharesTab.IncomingShares.ordinal,
            onTabSelected = {
                selectedTab = SharesTab.fromOrdinal(it)
                true
            }
        )
    }

    HandleNodeOptionEvent(
        megaNavigator = megaNavigator,
        nodeActionState = nodeActionState,
        nameCollisionLauncher = nameCollisionLauncher,
        snackbarHostState = snackbarHostState,
        onNodeNameCollisionResultHandled = nodeOptionsActionViewModel::markHandleNodeNameCollisionResult,
        onInfoToShowEventConsumed = nodeOptionsActionViewModel::onInfoToShowEventConsumed,
        onForeignNodeDialogShown = nodeOptionsActionViewModel::markForeignNodeDialogShown,
        onQuotaDialogShown = nodeOptionsActionViewModel::markQuotaDialogShown,
        onHandleNodesWithoutConflict = { collisionType, nodes ->
            when (collisionType) {
                NodeNameCollisionType.MOVE -> nodeOptionsActionViewModel.moveNodes(nodes)
                NodeNameCollisionType.COPY -> nodeOptionsActionViewModel.copyNodes(nodes)
                else -> { /* No-op for other types */
                }
            }
        },
    )

    LaunchedEffect(selectedItemsCount) {
        nodeOptionsActionViewModel.updateSelectionModeAvailableActions(
            selectedNodes = getSelectedNodes().toSet(),
            nodeSourceType = selectedTab.toNodeSourceType()
        )
    }

    EventEffect(
        event = nodeActionState.downloadEvent,
        onConsumed = nodeOptionsActionViewModel::markDownloadEventConsumed,
        action = onTransfer
    )

    // Reset selection mode after handling move, copy, delete action
    LaunchedEffect(nodeActionState.infoToShowEvent) {
        if (nodeActionState.infoToShowEvent is StateEventWithContentTriggered) {
            deselectAllItems()
        }
    }

    // Reset selection mode after handling name collision
    LaunchedEffect(nodeActionState.nodeNameCollisionsResult) {
        if (nodeActionState.nodeNameCollisionsResult is StateEventWithContentTriggered) {
            deselectAllItems()
        }
    }

    EventEffect(
        event = nodeActionState.renameNodeRequestEvent,
        onConsumed = nodeOptionsActionViewModel::resetRenameNodeRequest,
        action = { nodeId ->
            deselectAllItems()
            navigationHandler.navigate(RenameNodeDialogNavKey(nodeId = nodeId.longValue))
        }
    )

    EventEffect(
        event = nodeActionState.shareFolderDialogEvent,
        onConsumed = nodeOptionsActionViewModel::resetShareFolderDialogEvent,
        action = { handles ->
            shareNodeHandles = handles
        }
    )

    EventEffect(
        event = nodeActionState.shareFolderEvent,
        onConsumed = nodeOptionsActionViewModel::resetShareFolderEvent,
        action = { handles ->
            shareFolderLauncher.launch(handles.toLongArray())
        }
    )

    // Node options modal
    LaunchedEffect(visibleNodeOptionId) {
        if (visibleNodeOptionId != null) {
            nodeOptionSheetState.show()
        } else {
            nodeOptionSheetState.hide()
        }
    }

    // Todo: We will remove this, and replace it with NavigationHandler
    visibleNodeOptionId?.let { nodeId ->
        MegaModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            sheetState = nodeOptionSheetState,
            onDismissRequest = {
                visibleNodeOptionId = null
            },
            bottomSheetBackground = MegaModalBottomSheetBackground.Surface1
        ) {
            NodeOptionsBottomSheetRoute(
                navigationHandler = navigationHandler,
                onDismiss = {
                    visibleNodeOptionId = null
                },
                nodeId = nodeId.longValue,
                nodeSourceType = selectedTab.toNodeSourceType(),
                onTransfer = onTransfer,
                actionHandler = nodeActionHandler,
                nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            )
        }
    }

    if (shareNodeHandles.isNotEmpty()) {
        ShareFolderDialogM3(
            nodeIds = shareNodeHandles.map { NodeId(it) },
            onDismiss = {
                shareNodeHandles = emptyList()
            },
            onConfirm = { nodes ->
                val handles = nodes.map { it.id.longValue }.toLongArray()
                shareFolderLauncher.launch(handles)
            }
        )
    }

    EventEffect(
        event = nodeActionState.contactsData,
        onConsumed = nodeOptionsActionViewModel::markShareFolderAccessDialogShown,
        action = { (contactData, isFromBackups, nodeHandles) ->
            navigationHandler.navigate(
                ShareFolderAccessDialogNavKey(
                    nodes = nodeHandles,
                    contacts = contactData.joinToString(separator = ","),
                    isFromBackups = isFromBackups
                )
            )
        },
    )

    if (showSortBottomSheet) {
        val selectedSortConfiguration = when (selectedTab) {
            SharesTab.IncomingShares -> incomingSharesUiState.selectedSortConfiguration
            SharesTab.OutgoingShares -> outgoingSharesUiState.selectedSortConfiguration
            SharesTab.Links -> linksUiState.selectedSortConfiguration
        }

        SortBottomSheet(
            title = stringResource(sharedR.string.action_sort_by_header),
            options = NodeSortOption.getOptionsForSourceType(selectedTab.toNodeSourceType()),
            sheetState = sortBottomSheetState,
            selectedSort = SortBottomSheetResult(
                sortOptionItem = selectedSortConfiguration.sortOption,
                sortDirection = selectedSortConfiguration.sortDirection
            ),
            onSortOptionSelected = { result ->
                result?.let {
                    val sortConfig = NodeSortConfiguration(
                        sortOption = it.sortOptionItem,
                        sortDirection = it.sortDirection
                    )
                    when (selectedTab) {
                        SharesTab.IncomingShares -> incomingSharesViewModel.setSortOrder(sortConfig)
                        SharesTab.OutgoingShares -> outgoingSharesViewModel.setSortOrder(sortConfig)
                        SharesTab.Links -> linksViewModel.setSortOrder(sortConfig)
                    }
                    showSortBottomSheet = false
                }
            },
            onDismissRequest = {
                showSortBottomSheet = false
            }
        )
    }
}

/**
 * Enum representing the different tabs in the Shares screen
 */
private enum class SharesTab {
    IncomingShares,
    OutgoingShares,
    Links;

    /**
     * Converts the tab to its corresponding NodeSourceType
     */
    fun toNodeSourceType(): NodeSourceType = when (this) {
        IncomingShares -> NodeSourceType.INCOMING_SHARES
        OutgoingShares -> NodeSourceType.OUTGOING_SHARES
        Links -> NodeSourceType.LINKS
    }

    companion object {
        /**
         * Gets the SharesTab from by its ordinal value
         */
        fun fromOrdinal(ordinal: Int) = entries.getOrNull(ordinal) ?: IncomingShares
    }
}
