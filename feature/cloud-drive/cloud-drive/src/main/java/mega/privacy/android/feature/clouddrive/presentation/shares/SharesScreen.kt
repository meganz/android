package mega.privacy.android.feature.clouddrive.presentation.shares

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.tabs.MegaCollapsibleTabRow
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.modifiers.excludingBottomPadding
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.navigation.getSearchNavKey
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
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.shared.nodes.components.NodeSelectionModeAppBar
import mega.privacy.android.shared.nodes.components.SortBottomSheet
import mega.privacy.android.shared.nodes.components.SortBottomSheetResult
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.SharesScreenEvent


/**
 * Shares screen containing incoming shares, outgoing shares and links tabs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SharesScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
    nodeOptionsActionViewModel: NodeOptionsActionViewModel =
        hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
            creationCallback = { it.create(null) }
        ),
    incomingSharesViewModel: IncomingSharesViewModel = hiltViewModel(),
    outgoingSharesViewModel: OutgoingSharesViewModel = hiltViewModel(),
    linksViewModel: LinksViewModel = hiltViewModel(),
) {
    val megaNavigator = rememberMegaNavigator()
    var selectedTab by rememberSaveable { mutableStateOf(SharesTab.IncomingShares) }
    val nodeActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val selectionModeActionHandler = rememberMultiNodeActionHandler(
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

    val (isInSelectionMode, selectedItemsCount) = when (selectedTab) {
        SharesTab.IncomingShares -> incomingSharesUiState.isInSelectionMode to incomingSharesUiState.selectedItemsCount
        SharesTab.OutgoingShares -> outgoingSharesUiState.isInSelectionMode to outgoingSharesUiState.selectedItemsCount
        SharesTab.Links -> linksUiState.isInSelectionMode to linksUiState.selectedItemsCount
    }

    val isAllItemsSelected = when (selectedTab) {
        SharesTab.IncomingShares -> incomingSharesUiState.isAllSelected
        SharesTab.OutgoingShares -> outgoingSharesUiState.isAllSelected
        SharesTab.Links -> linksUiState.isAllSelected
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

    LaunchedOnceEffect {
        Analytics.tracker.trackEvent(SharesScreenEvent)
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            if (isInSelectionMode) {
                NodeSelectionModeAppBar(
                    modifier = Modifier.testTag(SHARES_SELECTION_MODE_APP_BAR_TAG),
                    count = selectedItemsCount,
                    isAllSelected = isAllItemsSelected,
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
                    modifier = Modifier.testTag(SHARES_MAIN_APP_BAR_TAG),
                    navigationType = AppBarNavigationType.Back {
                        navigationHandler.back()
                    },
                    title = stringResource(sharedR.string.video_section_videos_location_option_shared_items),
                    trailingIcons = {
                        TransfersToolbarWidget {
                            navigationHandler.navigate(TransfersNavKey())
                        }
                    },
                    actions = buildList {
                        add(
                            MenuActionWithClick(CommonMenuAction.Search) {
                                val searchNavKey = selectedTab.toNodeSourceType().getSearchNavKey(
                                    folderId = -1L,
                                    isRevampEnabled = incomingSharesUiState.isSearchRevampEnabled
                                )
                                navigationHandler.navigate(searchNavKey)
                            }
                        )
                    },
                )
            }
        },
        bottomBar = {
            NodeSelectionModeBottomBar(
                modifier = Modifier.testTag(SHARES_SELECTION_MODE_BOTTOM_BAR_TAG),
                availableActions = nodeActionState.availableActions,
                visibleActions = nodeActionState.visibleActions,
                visible = nodeActionState.visibleActions.isNotEmpty() && isInSelectionMode,
                multiNodeActionHandler = selectionModeActionHandler,
                selectedNodes = getSelectedNodes(),
                isSelecting = false,
            )
        },
    ) { paddingValues ->
        MegaCollapsibleTabRow(
            modifier = Modifier
                .fillMaxSize()
                .testTag(SHARES_TAB_ROW_TAG),
            contentPadding = paddingValues.excludingBottomPadding(),
            beyondViewportPageCount = 1,
            hideTabs = isInSelectionMode,
            pagerScrollEnabled = !isInSelectionMode,
            cells = {
                addTextTabWithScrollableContent(
                    tabItem = TabItems(
                        title = stringResource(sharedR.string.shares_screen_incoming_shares_tab_title),
                        testTag = SHARES_INCOMING_TAB_TAG
                    ),
                ) { _, modifier ->
                    IncomingSharesContent(
                        modifier = modifier,
                        uiState = incomingSharesUiState,
                        navigationHandler = navigationHandler,
                        onAction = incomingSharesViewModel::processAction,
                        onShowNodeOptions = { nodeId ->
                            navigationHandler.navigate(
                                NodeOptionsBottomSheetNavKey(
                                    nodeHandle = nodeId.longValue,
                                    nodeSourceType = NodeSourceType.INCOMING_SHARES,
                                    partiallyExpand = false
                                )
                            )
                        },
                        onSortOrderClick = { showSortBottomSheet = true },
                        contentPadding = PaddingValues(
                            bottom = paddingValues.calculateBottomPadding()
                        ),
                    )
                }
                addTextTabWithScrollableContent(
                    tabItem = TabItems(
                        title = stringResource(sharedR.string.shares_screen_outgoing_shares_tab_title),
                        testTag = SHARES_OUTGOING_TAB_TAG
                    ),
                ) { _, modifier ->
                    OutgoingSharesContent(
                        modifier = modifier,
                        uiState = outgoingSharesUiState,
                        navigationHandler = navigationHandler,
                        onAction = outgoingSharesViewModel::processAction,
                        onShowNodeOptions = { nodeId ->
                            navigationHandler.navigate(
                                NodeOptionsBottomSheetNavKey(
                                    nodeHandle = nodeId.longValue,
                                    nodeSourceType = NodeSourceType.OUTGOING_SHARES
                                )
                            )
                        },
                        onSortOrderClick = { showSortBottomSheet = true },
                        contentPadding = PaddingValues(
                            bottom = paddingValues.calculateBottomPadding()
                        ),
                    )
                }
                addTextTabWithScrollableContent(
                    tabItem = TabItems(
                        title = stringResource(sharedR.string.shares_screen_links_shares_tab_title),
                        testTag = SHARES_LINKS_TAB_TAG
                    ),
                ) { _, modifier ->
                    LinksContent(
                        modifier = modifier,
                        uiState = linksUiState,
                        navigationHandler = navigationHandler,
                        onAction = linksViewModel::processAction,
                        onShowNodeOptions = { nodeId ->
                            navigationHandler.navigate(
                                NodeOptionsBottomSheetNavKey(
                                    nodeHandle = nodeId.longValue,
                                    nodeSourceType = NodeSourceType.LINKS
                                )
                            )
                        },
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

    EventEffect(
        nodeActionState.actionTriggeredEvent,
        nodeOptionsActionViewModel::resetActionTriggered
    ) {
        deselectAllItems()
    }

    LaunchedEffect(selectedItemsCount) {
        nodeOptionsActionViewModel.updateSelectionModeAvailableActions(
            selectedNodes = getSelectedNodes().toSet(),
            nodeSourceType = selectedTab.toNodeSourceType()
        )
    }

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

internal const val SHARES_MAIN_APP_BAR_TAG = "shares_screen:main_app_bar"
internal const val SHARES_SELECTION_MODE_APP_BAR_TAG = "shares_screen:selection_mode_app_bar"
internal const val SHARES_SELECTION_MODE_BOTTOM_BAR_TAG =
    "shares_screen:selection_mode_bottom_bar"
internal const val SHARES_TAB_ROW_TAG = "shares_screen:tab_row"
internal const val SHARES_INCOMING_TAB_TAG = "shares_screen:incoming_tab"
internal const val SHARES_OUTGOING_TAB_TAG = "shares_screen:outgoing_tab"
internal const val SHARES_LINKS_TAB_TAG = "shares_screen:links_tab"
