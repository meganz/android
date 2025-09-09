package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.AddContentFab
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeAppBar
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import mega.privacy.android.core.nodecomponents.sheet.nodeactions.NodeActionUiOption
import mega.privacy.android.core.nodecomponents.sheet.nodeactions.NodeMoreOptionsBottomSheet
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.model.CloudDriveAppBarAction
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.DeselectAllItems
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.SelectAllItems
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.CloudDriveContent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.shared.original.core.ui.model.TopAppBarActionWithClick
import timber.log.Timber

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
    var showMoreBottomSheet by remember { mutableStateOf(false) }
    val moreOptionsBottomSheetState = rememberModalBottomSheetState()
    val nodeOptionsActionViewModel: NodeOptionsActionViewModel = hiltViewModel()
    val nodeOptionsActionUiState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val nodeActionHandler = rememberNodeActionHandler(nodeOptionsActionViewModel)

    BackHandler(enabled = uiState.isInSelectionMode) {
        viewModel.processAction(DeselectAllItems)
    }

    LaunchedEffect(uiState.items) {
        nodeOptionsActionViewModel.updateSelectedNodes(uiState.selectedNodes)
        Timber.d("Selected nodes: ${nodeOptionsActionViewModel.uiState.value.selectedNodes}")
    }

    EventEffect(
        event = nodeOptionsActionUiState.downloadEvent,
        onConsumed = nodeOptionsActionViewModel::markDownloadEventConsumed,
        action = onTransfer
    )

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
                    actions = buildList {
                        if (uiState.items.isNotEmpty()) {
                            add(
                                TopAppBarActionWithClick(CloudDriveAppBarAction.Search) {
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
                                TopAppBarActionWithClick(
                                    CloudDriveAppBarAction.More
                                ) {
                                    // TODO Open node options bottom sheet
                                }
                            )
                        }
                    },
                )
            }
        },
        bottomBar = {
            NodeSelectionModeBottomBar(
                count = uiState.selectedItemsCount,
                visible = uiState.isInSelectionMode,
                onActionPressed = { action ->
                    when (action) {
                        is NodeSelectionAction.More -> {
                            showMoreBottomSheet = true
                        }
                    }
                }
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
                nodeActionHandler = nodeActionHandler
            )
        }
    )

    CloudDriveScanDocumentHandler(
        cloudDriveUiState = uiState,
        cloudDriveViewModel = viewModel,
    )

    if (showMoreBottomSheet) {
        NodeMoreOptionsBottomSheet(
            options = NodeActionUiOption.defaults,
            sheetState = moreOptionsBottomSheetState,
            onDismissRequest = {
                showMoreBottomSheet = false
            },
            onOptionSelected = { option ->
                // Todo
                showMoreBottomSheet = false
            }
        )
    }
}
