package mega.privacy.android.feature.clouddrive.presentation.audio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.audio.model.AudioUiState
import mega.privacy.android.feature.clouddrive.presentation.audio.view.AudioContent
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.navigation.contract.state.ReportSelectionMode
import mega.privacy.android.navigation.destination.LegacySearchNavKey
import mega.privacy.android.navigation.destination.SearchNavKey
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.shared.nodes.components.NodeSelectionModeAppBar
import mega.privacy.android.shared.nodes.selection.rememberNodeSelectionState
import mega.privacy.android.shared.resources.R
import mega.privacy.mobile.analytics.event.BackButtonPressedEvent

/**
 * Audio Screen, used to display all audio nodes
 *
 * @param navigationHandler Navigation handler for screen navigation
 * @param onTransfer Callback to handle transfer events
 * @param megaNavigator Navigator for node action handlers; default uses [rememberMegaNavigator].
 * @param megaActivityResultContract Activity result contracts for selection actions; default resolves from context.
 * @param viewModel ViewModel for managing the state of the Audio screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
    nodeOptionsActionViewModel: NodeOptionsActionViewModel,
    modifier: Modifier = Modifier,
    megaNavigator: MegaNavigator = rememberMegaNavigator(),
    viewModel: AudioViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nodeOptionsActionUiState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val selectionModeActionHandler = rememberMultiNodeActionHandler(
        navigationHandler = navigationHandler,
        viewModel = nodeOptionsActionViewModel,
        megaNavigator = megaNavigator,
    )

    val selectionState = rememberNodeSelectionState()
    ReportSelectionMode(isInSelectionMode = selectionState.isInSelectionMode)
    val dataState = uiState as? AudioUiState.Data

    val selectedItemsCount by remember {
        derivedStateOf {
            val data = uiState as? AudioUiState.Data
            data?.computeSelectedItemsCount(selectedIds = selectionState.selectedNodeIds) ?: 0
        }
    }
    val isAllSelected by remember {
        derivedStateOf {
            val data = uiState as? AudioUiState.Data
            data?.let {
                selectedItemsCount == it.visibleItemsCount && it.visibleItemsCount > 0
            } ?: false
        }
    }
    val selectedNodes by remember {
        derivedStateOf {
            val ids = selectionState.selectedNodeIds
            val data = uiState as? AudioUiState.Data
            data?.items?.mapNotNull { item ->
                if (item.node.id in ids) item.node else null
            } ?: emptyList()
        }
    }

    BackHandler(enabled = selectionState.isInSelectionMode) {
        selectionState.deselectAll()
    }

    EventEffect(
        event = nodeOptionsActionUiState.actionTriggeredEvent,
        onConsumed = nodeOptionsActionViewModel::resetActionTriggered,
        action = {
            selectionState.deselectAll()
        }
    )

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (selectionState.isInSelectionMode) {
                NodeSelectionModeAppBar(
                    count = selectedItemsCount,
                    isAllSelected = isAllSelected,
                    isSelecting = false,
                    onSelectAllClicked = {
                        val allIds = dataState?.items?.map { it.node.id }?.toSet() ?: emptySet()
                        selectionState.selectAll(allIds)
                    },
                    onCancelSelectionClicked = { selectionState.deselectAll() }
                )
            } else {
                MegaTopAppBar(
                    title = stringResource(R.string.home_screen_audios_chip_title),
                    navigationType = AppBarNavigationType.Back {
                        Analytics.tracker.trackEvent(BackButtonPressedEvent)
                        navigationHandler.back()
                    },
                    trailingIcons = {
                        TransfersToolbarWidget {
                            navigationHandler.navigate(TransfersNavKey())
                        }
                    },
                    actions = buildList {
                        if (dataState != null && !dataState.isEmpty) {
                            add(
                                MenuActionWithClick(CommonMenuAction.Search) {
                                    val searchNavKey = if (dataState.isSearchRevampEnabled) {
                                        SearchNavKey(
                                            parentHandle = -1L,
                                            nodeSourceType = NodeSourceType.AUDIO
                                        )
                                    } else {
                                        LegacySearchNavKey(
                                            parentHandle = -1L,
                                            nodeSourceType = NodeSourceType.AUDIO
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
                visible = nodeOptionsActionUiState.visibleActions.isNotEmpty() &&
                        selectionState.isInSelectionMode,
                multiNodeActionHandler = selectionModeActionHandler,
                selectedNodes = selectedNodes,
                isSelecting = false,
            )
        },
        content = { innerPadding ->
            AudioContent(
                navigationHandler = navigationHandler,
                uiState = uiState,
                contentPadding = innerPadding,
                onAction = viewModel::processAction,
                onTransfer = onTransfer,
                onSortNodes = viewModel::setCloudSortOrder,
                showNodeOptionsBottomSheet = navigationHandler::navigate,
                nodeOptionsActionViewModel = nodeOptionsActionViewModel,
                selectionState = selectionState,
                isInSelectionMode = selectionState.isInSelectionMode,
                selectedNodes = selectedNodes,
            )
        },
    )
}
