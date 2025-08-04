package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.LoadingView
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.fab.MegaFab
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.list.view.NodesView
import mega.privacy.android.core.nodecomponents.selectionmode.NodeSelectionModeAppBar
import mega.privacy.android.core.nodecomponents.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.sheet.upload.UploadOptionsBottomSheet
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.model.CloudDriveAppBarAction
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.ChangeViewTypeClicked
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.DeselectAllItems
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.ItemClicked
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.ItemLongClicked
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.NavigateBackEventConsumed
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.NavigateToFolderEventConsumed
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.OpenedFileNodeHandled
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.SelectAllItems
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveUiState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.model.TopAppBarActionWithClick

/**
 * Cloud Drive Screen, used to display contents of a folder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDriveScreen(
    onBack: () -> Unit,
    onNavigateToFolder: (NodeId) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    viewModel: CloudDriveViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showUploadOptionsBottomSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isInSelectionMode) {
        viewModel.processAction(DeselectAllItems)
    }

    MegaScaffold(
        topBar = {
            if (uiState.isInSelectionMode) {
                NodeSelectionModeAppBar(
                    count = uiState.selectedNodeIds.size,
                    onSelectAllClicked = { viewModel.processAction(SelectAllItems) },
                    onCancelSelectionClicked = { viewModel.processAction(DeselectAllItems) }
                )
            } else {
                MegaTopAppBar(
                    title = uiState.title.text,
                    navigationType = AppBarNavigationType.Back(onBack),
                    actions = buildList {
                        when {
                            uiState.items.isNotEmpty() -> add(
                                TopAppBarActionWithClick(CloudDriveAppBarAction.Search) {
                                    // TODO Handle search
                                })
                        }
                    },
                )
            }
        },
        bottomBar = {
            NodeSelectionModeBottomBar(
                count = uiState.selectedNodeIds.size,
                visible = uiState.isInSelectionMode,
                onActionPressed = {
                    // TODO
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isInSelectionMode && !uiState.items.isEmpty()) {
                MegaFab(
                    onClick = { showUploadOptionsBottomSheet = true },
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Plus)
                )
            }
        },
        content = { innerPadding ->
            CloudDriveContent(
                uiState = uiState,
                contentPadding = innerPadding,
                onAction = viewModel::processAction,
                onNavigateToFolder = onNavigateToFolder,
                onNavigateBack = onBack,
                onTransfer = onTransfer
            )
        }
    )

    if (showUploadOptionsBottomSheet) {
        UploadOptionsBottomSheet(
            onUploadFilesClicked = {
                // TODO: Handle upload files
            },
            onUploadFolderClicked = {
                // TODO: Handle upload folder
            },
            onScanDocumentClicked = {
                // TODO: Handle scan document
            },
            onCaptureClicked = {
                // TODO: Handle capture
            },
            onNewFolderClicked = {
                // TODO: Handle new folder
            },
            onNewTextFileClicked = {
                // TODO: Handle new text file
            },
            onDismissSheet = {
                showUploadOptionsBottomSheet = false
            }
        )
    }
}

@Composable
internal fun CloudDriveContent(
    uiState: CloudDriveUiState,
    onAction: (CloudDriveAction) -> Unit,
    onNavigateToFolder: (NodeId) -> Unit,
    onNavigateBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp, 0.dp),
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val coroutineScope = rememberCoroutineScope()
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
}