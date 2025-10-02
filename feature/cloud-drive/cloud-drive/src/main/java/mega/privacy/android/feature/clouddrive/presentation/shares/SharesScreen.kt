package mega.privacy.android.feature.clouddrive.presentation.shares

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.tabs.MegaScrollableTabRow
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.TabItems
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeAppBar
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.IncomingSharesContent
import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.IncomingSharesViewModel
import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.model.IncomingSharesAction
import mega.privacy.android.feature.clouddrive.presentation.shares.outgoingshares.OutgoingSharesContent
import mega.privacy.android.feature.clouddrive.presentation.shares.outgoingshares.OutgoingSharesViewModel
import mega.privacy.android.feature.clouddrive.presentation.shares.outgoingshares.model.OutgoingSharesAction
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.extensions.rememberMegaNavigator

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
) {
    val megaNavigator = rememberMegaNavigator()
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val nodeOptionsActionUiState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val nodeActionHandler = rememberNodeActionHandler(
        navigationHandler = navigationHandler,
        viewModel = nodeOptionsActionViewModel,
        megaNavigator = megaNavigator
    )
    val incomingSharesUiState by incomingSharesViewModel.uiState.collectAsStateWithLifecycle()
    val outgoingSharesUiState by outgoingSharesViewModel.uiState.collectAsStateWithLifecycle()

    val (isInSelectionMode, selectedItemsCount) = when (selectedTabIndex) {
        0 -> incomingSharesUiState.isInSelectionMode to incomingSharesUiState.selectedItemsCount
        1 -> outgoingSharesUiState.isInSelectionMode to outgoingSharesUiState.selectedItemsCount
        else -> false to 0 // TODO Selection mode for links
    }

    fun deselectAllItems() {
        when (selectedTabIndex) {
            0 -> incomingSharesViewModel.processAction(IncomingSharesAction.DeselectAllItems)
            1 -> outgoingSharesViewModel.processAction(OutgoingSharesAction.DeselectAllItems)
            else -> {} // TODO Selection mode for links
        }
    }

    fun selectAllItems() {
        when (selectedTabIndex) {
            0 -> incomingSharesViewModel.processAction(IncomingSharesAction.SelectAllItems)
            1 -> outgoingSharesViewModel.processAction(OutgoingSharesAction.SelectAllItems)
            else -> {} // TODO Selection mode for links
        }
    }

    fun getSelectedNodes() = if (isInSelectionMode) {
        when (selectedTabIndex) {
            0 -> incomingSharesUiState.selectedNodes
            1 -> outgoingSharesUiState.selectedNodes
            else -> emptyList() // TODO Selection mode for links
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
        contentWindowInsets = WindowInsets.statusBars,
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
                )
            }
        },
        bottomBar = {
            NodeSelectionModeBottomBar(
                availableActions = nodeOptionsActionUiState.availableActions,
                visibleActions = nodeOptionsActionUiState.visibleActions,
                visible = nodeOptionsActionUiState.visibleActions.isNotEmpty() && isInSelectionMode,
                nodeActionHandler = nodeActionHandler,
                selectedNodes = getSelectedNodes(),
                isSelecting = false
            )
        },
    ) { paddingValues ->
        MegaScrollableTabRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
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
                        onAction = incomingSharesViewModel::processAction,
                        navigationHandler = navigationHandler,
                    )
                }
                addTextTabWithScrollableContent(
                    tabItem = TabItems(stringResource(R.string.tab_outgoing_shares)),
                ) { _, modifier ->
                    OutgoingSharesContent(
                        modifier = modifier,
                        uiState = outgoingSharesUiState,
                        onAction = outgoingSharesViewModel::processAction,
                        navigationHandler = navigationHandler
                    )
                }
                addTextTabWithScrollableContent(
                    tabItem = TabItems(stringResource(R.string.tab_links_shares)),
                ) { _, modifier ->
                    // TODO
                    Box(
                        modifier = modifier
                            .fillMaxSize()
                            .align(Alignment.Center)
                            .padding(paddingValues)
                    ) {
                        MegaText("Links Screen")
                    }
                }
            },
            initialSelectedIndex = 0,
            onTabSelected = {
                selectedTabIndex = it
                true
            }
        )
    }

    EventEffect(
        event = nodeOptionsActionUiState.downloadEvent,
        onConsumed = nodeOptionsActionViewModel::markDownloadEventConsumed,
        action = onTransfer
    )

    LaunchedEffect(selectedItemsCount) {
        nodeOptionsActionViewModel.updateSelectionModeAvailableActions(
            getSelectedNodes().toSet(),
            NodeSourceType.CLOUD_DRIVE
        )
    }
}