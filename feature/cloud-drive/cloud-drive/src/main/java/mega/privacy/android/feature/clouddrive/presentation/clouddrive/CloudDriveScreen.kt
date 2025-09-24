package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionIconWithClick
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.AddContentFab
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeAppBar
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.model.CloudDriveAppBarAction
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.DeselectAllItems
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.SelectAllItems
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.CloudDriveContent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.extensions.rememberMegaNavigator

/**
 * Cloud Drive Screen, used to display contents of a folder
 *
 * @param onBack Callback to be invoked when the back button is pressed
 * @param onNavigateToFolder Callback to navigate to a specific folder, accepts NodeId and an name for folder title
 * @param onCreatedNewFolder Callback to be invoked when a new folder is created, accepts NodeId of the new folder
 * @param onTransfer Callback to handle transfer events
 * @param viewModel ViewModel for managing the state of the Cloud Drive screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDriveScreen(
    navigationHandler: NavigationHandler,
    onBack: () -> Unit,
    onNavigateToFolder: (NodeId, String?) -> Unit,
    onCreatedNewFolder: (NodeId) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onRenameNode: (NodeId) -> Unit,
    viewModel: CloudDriveViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showUploadOptionsBottomSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val megaNavigator = rememberMegaNavigator()
    val nodeOptionsActionViewModel: NodeOptionsActionViewModel = hiltViewModel()
    val nodeOptionsActionUiState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val nodeActionHandler = rememberNodeActionHandler(
        navigationHandler = navigationHandler,
        viewModel = nodeOptionsActionViewModel,
        megaNavigator = megaNavigator,
    )
    // Controls the visibility of the Node Options Bottom Sheet on CloudDriveContent
    var visibleNodeOptionId: NodeId? by remember { mutableStateOf(null) }

    BackHandler(enabled = uiState.isInSelectionMode) {
        viewModel.processAction(DeselectAllItems)
    }

    BackHandler(enabled = uiState.isInSelectionMode) {
        viewModel.processAction(DeselectAllItems)
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            if (uiState.isInSelectionMode) {
                NodeSelectionModeAppBar(
                    count = uiState.selectedItemsCount,
                    isSelecting = uiState.isSelecting,
                    onSelectAllClicked = { viewModel.processAction(SelectAllItems) },
                    onCancelSelectionClicked = { viewModel.processAction(DeselectAllItems) }
                )
            } else {
                MegaTopAppBar(
                    title = uiState.title.text,
                    navigationType = AppBarNavigationType.Back(onBack),
                    trailingIcons = { TransfersToolbarWidget(navigationHandler) },
                    actions = buildList {
                        if (uiState.items.isNotEmpty()) {
                            add(
                                MenuActionIconWithClick(CloudDriveAppBarAction.Search) {
                                    megaNavigator.openSearchActivity(
                                        context = context,
                                        nodeSourceType = viewModel.nodeSourceType,
                                        parentHandle = uiState.currentFolderId.longValue,
                                        isFirstNavigationLevel = false
                                    )
                                }
                            )
                        }

                        if (!uiState.isCloudDriveRoot) {
                            add(
                                MenuActionIconWithClick(
                                    CloudDriveAppBarAction.More
                                ) {
                                    visibleNodeOptionId = uiState.currentFolderId
                                }
                            )
                        }
                    },
                )
            }
        },
        bottomBar = {
            NodeSelectionModeBottomBar(
                availableActions = nodeOptionsActionUiState.availableActions,
                visibleActions = nodeOptionsActionUiState.visibleActions,
                visible = nodeOptionsActionUiState.visibleActions.isNotEmpty() && uiState.isInSelectionMode,
                nodeActionHandler = nodeActionHandler,
                selectedNodes = uiState.selectedNodes,
                isSelecting = uiState.isSelecting
            )
        },
        floatingActionButton = {
            AddContentFab(
                visible = !uiState.isInSelectionMode,
                onClick = { showUploadOptionsBottomSheet = true }
            )
        },
        content = { innerPadding ->
            CloudDriveContent(
                isTabContent = false,
                navigationHandler = navigationHandler,
                uiState = uiState,
                showUploadOptionsBottomSheet = showUploadOptionsBottomSheet,
                onDismissUploadOptionsBottomSheet = { showUploadOptionsBottomSheet = false },
                contentPadding = innerPadding,
                onAction = viewModel::processAction,
                onNavigateToFolder = onNavigateToFolder,
                onNavigateBack = onBack,
                onTransfer = onTransfer,
                onCreatedNewFolder = onCreatedNewFolder,
                onRenameNode = onRenameNode,
                onSortNodes = viewModel::setCloudSortOrder,
                nodeOptionsActionViewModel = nodeOptionsActionViewModel,
                nodeActionHandler = nodeActionHandler,
                visibleParentNodeOptionId = visibleNodeOptionId,
                onDismissNodeOptionsBottomSheet = { visibleNodeOptionId = null }
            )
        }
    )

    CloudDriveScanDocumentHandler(
        cloudDriveUiState = uiState,
        cloudDriveViewModel = viewModel,
    )
}
