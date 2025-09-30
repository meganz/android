package mega.privacy.android.feature.clouddrive.presentation.shares

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
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
import mega.privacy.android.navigation.destination.CloudDriveNavKey
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

    // TODO handle back press in selection mode

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            // TODO Selection mode
            MegaTopAppBar(
                navigationType = AppBarNavigationType.Back {
                    navigationHandler.back()
                },
                title = stringResource(R.string.title_shared_items),
                trailingIcons = {
                    TransfersToolbarWidget(navigationHandler)
                },
            )
        },
        bottomBar = {
            // TODO Selection mode
        },
        floatingActionButton = {
            // TODO FAB
        },
    ) { paddingValues ->
        MegaScrollableTabRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            beyondViewportPageCount = 1,
            hideTabs = false,
            pagerScrollEnabled = true,
            cells = {
                addTextTabWithScrollableContent(
                    tabItem = TabItems(stringResource(R.string.tab_incoming_shares)),
                ) { _, modifier ->
                    IncomingSharesContent(
                        modifier = modifier,
                        uiState = incomingSharesUiState,
                        onAction = incomingSharesViewModel::processAction,
                    )
                }
                addTextTabWithScrollableContent(
                    tabItem = TabItems(stringResource(R.string.tab_outgoing_shares)),
                ) { _, modifier ->
                    OutgoingSharesContent(
                        modifier = modifier,
                        uiState = outgoingSharesUiState,
                        onAction = outgoingSharesViewModel::processAction,
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
        event = incomingSharesUiState.navigateToFolderEvent,
        onConsumed = { incomingSharesViewModel.processAction(IncomingSharesAction.NavigateToFolderEventConsumed) }
    ) { node ->
        navigationHandler.navigate(
            CloudDriveNavKey(
                nodeHandle = node.id.longValue,
                nodeName = node.name,
                nodeSourceType = NodeSourceType.INCOMING_SHARES
            )
        )
    }

    EventEffect(
        event = outgoingSharesUiState.navigateToFolderEvent,
        onConsumed = { outgoingSharesViewModel.processAction(OutgoingSharesAction.NavigateToFolderEventConsumed) }
    ) { node ->
        navigationHandler.navigate(
            CloudDriveNavKey(
                nodeHandle = node.id.longValue,
                nodeName = node.name,
                nodeSourceType = NodeSourceType.OUTGOING_SHARES
            )
        )
    }
}