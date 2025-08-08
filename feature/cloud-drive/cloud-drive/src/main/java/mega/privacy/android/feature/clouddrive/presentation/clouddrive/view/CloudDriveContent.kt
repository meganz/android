package mega.privacy.android.feature.clouddrive.presentation.clouddrive.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LoadingView
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.dialog.textfile.NewTextFileNodeDialog
import mega.privacy.android.core.nodecomponents.dialog.newfolderdialog.NewFolderNodeDialog
import mega.privacy.android.core.nodecomponents.list.view.NodesView
import mega.privacy.android.core.nodecomponents.sheet.upload.UploadOptionsBottomSheet
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.ChangeViewTypeClicked
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.ItemClicked
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.ItemLongClicked
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.NavigateBackEventConsumed
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.NavigateToFolderEventConsumed
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.OpenedFileNodeHandled
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveUiState
import mega.privacy.android.feature.clouddrive.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CloudDriveContent(
    uiState: CloudDriveUiState,
    showUploadOptionsBottomSheet: Boolean,
    onDismissUploadOptionsBottomSheet: () -> Unit,
    onAction: (CloudDriveAction) -> Unit,
    onNavigateToFolder: (NodeId) -> Unit,
    onNavigateBack: () -> Unit,
    onCreatedNewFolder: (NodeId) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onAddFilesClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp, 0.dp),
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
) {
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewTextFileDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackBarHostState.current

    when {
        uiState.isLoading -> {
            if (uiState.currentFolderId.longValue == -1L) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()

                ) {
                    LoadingView(Modifier.align(alignment = Alignment.Center))
                }
            }
        }

        uiState.isEmpty -> {
            CloudDriveEmptyView(
                onAddFilesClick = onAddFilesClick
            )
        }

        else -> NodesView(
            modifier = modifier,
            listContentPadding = contentPadding,
            listState = listState,
            gridState = gridState,
            items = uiState.items,
            isHiddenNodesEnabled = uiState.isHiddenNodesEnabled,
            showHiddenNodes = uiState.showHiddenNodes,
            onMenuClick = { },
            onItemClicked = { onAction(ItemClicked(it)) },
            onLongClicked = { onAction(ItemLongClicked(it)) },
            sortOrder = "Name",
            isListView = uiState.currentViewType == ViewType.LIST,
            onSortOrderClick = {},
            onChangeViewTypeClicked = { onAction(ChangeViewTypeClicked) },
            onLinkClicked = {},
            onDisputeTakeDownClicked = {},
            showMediaDiscoveryButton = false,
            onEnterMediaDiscoveryClick = {},
            inSelectionMode = uiState.isInSelectionMode,
        )
    }

    EventEffect(
        event = uiState.navigateToFolderEvent,
        onConsumed = { onAction(NavigateToFolderEventConsumed) }
    ) { nodeId ->
        onNavigateToFolder(nodeId)
    }

    EventEffect(
        event = uiState.navigateBack,
        onConsumed = { onAction(NavigateBackEventConsumed) }
    ) {
        onNavigateBack()
    }

    uiState.openedFileNode?.let { openedFileNode ->
        HandleNodeAction3(
            typedFileNode = openedFileNode,
            snackBarHostState = snackbarHostState,
            coroutineScope = coroutineScope,
            onActionHandled = { onAction(OpenedFileNodeHandled) },
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            onDownloadEvent = onTransfer,
            sortOrder = SortOrder.ORDER_NONE
        )
    }

    if (showUploadOptionsBottomSheet) {
        UploadOptionsBottomSheet(
            onUploadFilesClicked = {
                // TODO: Handle upload files
            },
            onUploadFolderClicked = {
                // TODO: Handle upload folder
            },
            onScanDocumentClicked = {
                onAction(CloudDriveAction.StartDocumentScanning)
            },
            onCaptureClicked = {
                // TODO: Handle capture
            },
            onNewFolderClicked = {
                showNewFolderDialog = true
            },
            onNewTextFileClicked = {
                showNewTextFileDialog = true
            },
            onDismissSheet = onDismissUploadOptionsBottomSheet
        )
    }

    if (showNewFolderDialog) {
        NewFolderNodeDialog(
            parentNode = uiState.currentFolderId,
            onCreateFolder = { folderId ->
                showNewFolderDialog = false
                coroutineScope.launch {
                    if (folderId != null) {
                        onCreatedNewFolder(folderId)
                    } else {
                        snackbarHostState?.showAutoDurationSnackbar(context.getString(R.string.context_folder_no_created))
                    }
                }
            },
            onDismiss = {
                showNewFolderDialog = false
            }
        )
    }

    if (showNewTextFileDialog) {
        NewTextFileNodeDialog(
            parentNode = uiState.currentFolderId,
            onDismiss = {
                showNewTextFileDialog = false
            }
        )
    }
}