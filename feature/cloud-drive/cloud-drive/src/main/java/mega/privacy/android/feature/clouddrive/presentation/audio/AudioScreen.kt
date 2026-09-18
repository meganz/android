package mega.privacy.android.feature.clouddrive.presentation.audio

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionActionsEffect
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
import mega.privacy.android.navigation.destination.SearchNavKey
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.shared.nodes.components.NodeSelectionModeAppBar
import mega.privacy.android.shared.nodes.selection.rememberNodeSelection
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

    val dataState = uiState as? AudioUiState.Data
    val selection = rememberNodeSelection(items = uiState.items)
    ReportSelectionMode(isInSelectionMode = selection.isInSelectionMode)
    NodeSelectionActionsEffect(
        selection = selection,
        nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        uiState = nodeOptionsActionUiState,
        onIntent = nodeOptionsActionViewModel::processIntent,
    )

    BackHandler(enabled = selection.isInSelectionMode) {
        selection.deselectAll()
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (selection.isInSelectionMode) {
                NodeSelectionModeAppBar(selection = selection)
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
                        if (dataState != null && !dataState.items.isEmpty()) {
                            add(
                                MenuActionWithClick(CommonMenuAction.Search) {
                                    val searchNavKey = SearchNavKey(
                                        parentHandle = -1L,
                                        nodeSourceType = NodeSourceType.AUDIO
                                    )
                                    navigationHandler.navigate(searchNavKey)
                                })
                        }
                    }
                )
            }
        },
        bottomBar = {
            @SuppressLint("ComposeViewModelForwarding")
            NodeSelectionModeBottomBar(
                availableActions = nodeOptionsActionUiState.availableActions,
                visibleActions = nodeOptionsActionUiState.visibleActions,
                visible = nodeOptionsActionUiState.visibleActions.isNotEmpty() &&
                        selection.isInSelectionMode,
                multiNodeActionHandler = selectionModeActionHandler,
                selectedNodes = selection.selectedNodes,
                isSelecting = selection.isSelecting,
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
                selection = selection,
            )
        },
    )
}
