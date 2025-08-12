package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.core.nodecomponents.components.AddContentFab
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeAppBar
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.model.CloudDriveAppBarAction
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.DeselectAllItems
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.SelectAllItems
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.CloudDriveContent
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.shared.original.core.ui.model.TopAppBarActionWithClick

/**
 * Cloud Drive Screen, used to display contents of a folder
 */
@Composable
fun CloudDriveScreen(
    onBack: () -> Unit,
    onNavigateToFolder: (NodeId) -> Unit,
    onCreatedNewFolder: (NodeId) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    viewModel: CloudDriveViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showUploadOptionsBottomSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val megaNavigator = rememberMegaNavigator()

    BackHandler(enabled = uiState.isInSelectionMode) {
        viewModel.processAction(DeselectAllItems)
    }

    MegaScaffold(
        topBar = {
            if (uiState.isInSelectionMode) {
                NodeSelectionModeAppBar(
                    count = uiState.selectedItemsCount,
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

                        if (!uiState.isRootCloudDrive) {
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
                onActionPressed = {
                    // TODO
                }
            )
        },
        floatingActionButton = {
            AddContentFab(
                visible = !uiState.isInSelectionMode && uiState.visibleItemsCount > 0,
                onClick = { showUploadOptionsBottomSheet = true }
            )
        },
        content = { innerPadding ->
            CloudDriveContent(
                uiState = uiState,
                showUploadOptionsBottomSheet = showUploadOptionsBottomSheet,
                onDismissUploadOptionsBottomSheet = { showUploadOptionsBottomSheet = false },
                contentPadding = innerPadding,
                onAction = viewModel::processAction,
                onNavigateToFolder = onNavigateToFolder,
                onNavigateBack = onBack,
                onTransfer = onTransfer,
                onAddFilesClick = { showUploadOptionsBottomSheet = true },
                onCreatedNewFolder = onCreatedNewFolder,
            )
        }
    )

    CloudDriveScanDocumentHandler(
        cloudDriveUiState = uiState,
        cloudDriveViewModel = viewModel,
    )
}
