package mega.privacy.android.feature.clouddrive.presentation.favourites

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeAppBar
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.sharedcomponents.menu.CommonAppBarAction
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.favourites.model.FavouritesAction.DeselectAllItems
import mega.privacy.android.feature.clouddrive.presentation.favourites.model.FavouritesAction.SelectAllItems
import mega.privacy.android.feature.clouddrive.presentation.favourites.view.FavouritesContent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.LegacySearchNavKey
import mega.privacy.android.navigation.destination.SearchNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.mobile.analytics.event.BackButtonPressedEvent
import mega.privacy.mobile.analytics.event.FavouritesScreenEvent

/**
 * Favourites Screen, used to display all favourite nodes
 *
 * @param onBack Callback to be invoked when the back button is pressed
 * @param onTransfer Callback to handle transfer events
 * @param viewModel ViewModel for managing the state of the Favourites screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
    viewModel: FavouritesViewModel = hiltViewModel(),
    nodeOptionsActionViewModel: NodeOptionsActionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val megaNavigator = rememberMegaNavigator()
    val nodeOptionsActionUiState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val selectionModeActionHandler = rememberMultiNodeActionHandler(
        navigationHandler = navigationHandler,
        viewModel = nodeOptionsActionViewModel,
        megaNavigator = megaNavigator,
    )

    BackHandler(enabled = uiState.isInSelectionMode) {
        viewModel.processAction(DeselectAllItems)
    }

    LifecycleResumeEffect(Unit) {
        Analytics.tracker.trackEvent(FavouritesScreenEvent)
        onPauseOrDispose {}
    }


    EventEffect(
        event = nodeOptionsActionUiState.actionTriggeredEvent,
        onConsumed = nodeOptionsActionViewModel::resetActionTriggered,
        action = {
            viewModel.processAction(DeselectAllItems)
        }
    )

    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            if (uiState.isInSelectionMode) {
                NodeSelectionModeAppBar(
                    count = uiState.selectedItemsCount,
                    isAllSelected = uiState.isAllSelected,
                    isSelecting = false,
                    onSelectAllClicked = { viewModel.processAction(SelectAllItems) },
                    onCancelSelectionClicked = { viewModel.processAction(DeselectAllItems) }
                )
            } else {
                MegaTopAppBar(
                    title = "Favourites",
                    navigationType = AppBarNavigationType.Back {
                        Analytics.tracker.trackEvent(BackButtonPressedEvent)
                        navigationHandler.back()
                    },
                    trailingIcons = { TransfersToolbarWidget(navigationHandler::navigate) },
                    actions = buildList {
                        if (!uiState.isEmpty) {
                            add(
                                MenuActionWithClick(CommonAppBarAction.Search) {
                                    val searchNavKey = if (uiState.isSearchRevampEnabled) {
                                        SearchNavKey(
                                            parentHandle = -1L,
                                            nodeSourceType = NodeSourceType.FAVOURITES
                                        )
                                    } else {
                                        LegacySearchNavKey(
                                            parentHandle = -1L,
                                            nodeSourceType = NodeSourceType.FAVOURITES
                                        )
                                    }
                                    navigationHandler.navigate(searchNavKey)
                                })
                        }
                    }
                )
            }
        },
        bottomBar = {
            NodeSelectionModeBottomBar(
                availableActions = nodeOptionsActionUiState.availableActions,
                visibleActions = nodeOptionsActionUiState.visibleActions,
                visible = nodeOptionsActionUiState.visibleActions.isNotEmpty() && uiState.isInSelectionMode,
                multiNodeActionHandler = selectionModeActionHandler,
                selectedNodes = uiState.selectedNodes,
                isSelecting = false,
            )
        },
        content = { innerPadding ->
            FavouritesContent(
                navigationHandler = navigationHandler,
                uiState = uiState,
                contentPadding = innerPadding,
                onAction = viewModel::processAction,
                onTransfer = onTransfer,
                onSortNodes = viewModel::setCloudSortOrder,
                showNodeOptionsBottomSheet = navigationHandler::navigate,
                nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            )
        }
    )
}
