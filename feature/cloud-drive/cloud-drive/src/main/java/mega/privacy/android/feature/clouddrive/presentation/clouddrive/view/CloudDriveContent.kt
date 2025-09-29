package mega.privacy.android.feature.clouddrive.presentation.clouddrive.view

import android.Manifest.permission.CAMERA
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.RECORD_AUDIO
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.banners.StorageOverQuotaBannerM3
import mega.privacy.android.core.nodecomponents.components.banners.StorageOverQuotaCapacity
import mega.privacy.android.core.nodecomponents.dialog.newfolderdialog.NewFolderNodeDialog
import mega.privacy.android.core.nodecomponents.dialog.textfile.NewTextFileNodeDialog
import mega.privacy.android.core.nodecomponents.list.NodesView
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.nodecomponents.list.rememberDynamicSpanCount
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetRoute
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheet
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheetResult
import mega.privacy.android.core.nodecomponents.sheet.upload.UploadOptionsBottomSheet
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.ChangeViewTypeClicked
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.ItemClicked
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.ItemLongClicked
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.NavigateBackEventConsumed
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.NavigateToFolderEventConsumed
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.OpenedFileNodeHandled
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveUiState
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.NodesLoadingState
import mega.privacy.android.feature.clouddrive.presentation.upload.UploadingFiles
import mega.privacy.android.navigation.ExtraConstant
import mega.privacy.android.navigation.camera.CameraArg
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.MediaDiscoveryNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.navigation.extensions.rememberMegaResultContract
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CloudDriveContent(
    isTabContent: Boolean,
    navigationHandler: NavigationHandler,
    uiState: CloudDriveUiState,
    showUploadOptionsBottomSheet: Boolean,
    onDismissUploadOptionsBottomSheet: () -> Unit,
    onAction: (CloudDriveAction) -> Unit,
    onNavigateToFolder: (NodeId, String?) -> Unit,
    onNavigateBack: () -> Unit,
    onCreatedNewFolder: (NodeId) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onRenameNode: (NodeId) -> Unit,
    onSortNodes: (NodeSortConfiguration) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp, 0.dp),
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    nodeOptionsActionViewModel: NodeOptionsActionViewModel = hiltViewModel(),
    nodeActionHandler: NodeActionHandler = rememberNodeActionHandler(
        navigationHandler = navigationHandler,
        viewModel = nodeOptionsActionViewModel
    ),
    visibleParentNodeOptionId: NodeId? = null,
    onDismissNodeOptionsBottomSheet: () -> Unit = {},
) {
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewTextFileDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackBarHostState.current
    val megaResultContract = rememberMegaResultContract()
    val megaNavigator = rememberMegaNavigator()
    var uploadUris by rememberSaveable { mutableStateOf(emptyList<Uri>()) }
    var isUploadFolder by rememberSaveable { mutableStateOf(false) }
    val nodeActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val nodeOptionsActionUiState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val internalFolderPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val intent = it.data
            val resultCode = it.resultCode
            if (intent != null && resultCode == Activity.RESULT_OK) {
                val result = intent.getStringExtra(ExtraConstant.EXTRA_ACTION_RESULT)
                if (!result.isNullOrEmpty()) {
                    coroutineScope.launch {
                        snackbarHostState?.showAutoDurationSnackbar(result)
                    }
                }
            }
        }
    val openMultipleDocumentLauncher =
        rememberLauncherForActivityResult(megaResultContract.openMultipleDocumentsPersistable) {
            if (it.isNotEmpty()) {
                uploadUris = it
            }
        }
    val uploadFolderLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val intent = it.data
            val uri = intent?.data
            if (it.resultCode == Activity.RESULT_OK && uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                megaNavigator.openInternalFolderPicker(
                    context = context,
                    isUpload = true,
                    parentId = uiState.currentFolderId,
                    initialUri = uri,
                    launcher = internalFolderPickerLauncher
                )
            }
        }
    val manualUploadFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        runCatching {
            if (isUploadFolder) {
                uploadFolderLauncher.launch(
                    Intent.createChooser(
                        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),
                        null
                    )
                )
            } else {
                openMultipleDocumentLauncher.launch(arrayOf("*/*"))
            }
        }.onFailure {
            Timber.e(it)
        }
    }
    val nameCollisionLauncher = rememberLauncherForActivityResult(
        contract = megaResultContract.nameCollisionActivityContract
    ) { message ->
        if (!message.isNullOrEmpty()) {
            coroutineScope.launch {
                snackbarHostState?.showAutoDurationSnackbar(message)
            }
        }
    }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = megaResultContract.inAppCameraResultContract
    ) { uri: Uri? ->
        if (uri != null) {
            uploadUris = listOf(uri)
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        if (permissionsResult[CAMERA] == true) {
            takePictureLauncher.launch(
                CameraArg(
                    title = context.getString(R.string.context_upload),
                    buttonText = context.getString(R.string.context_upload)
                )
            )
        } else {
            coroutineScope.launch {
                snackbarHostState?.showAutoDurationSnackbar(context.getString(R.string.chat_attach_pick_from_camera_deny_permission))
            }
        }
    }
    HandleNodeOptionEvent(
        megaNavigator = megaNavigator,
        nodeActionState = nodeActionState,
        nameCollisionLauncher = nameCollisionLauncher,
        snackbarHostState = snackbarHostState,
        onNodeNameCollisionResultHandled = nodeOptionsActionViewModel::markHandleNodeNameCollisionResult,
        onInfoToShowEventConsumed = nodeOptionsActionViewModel::onInfoToShowEventConsumed,
        onForeignNodeDialogShown = nodeOptionsActionViewModel::markForeignNodeDialogShown,
        onQuotaDialogShown = nodeOptionsActionViewModel::markQuotaDialogShown,
        onHandleNodesWithoutConflict = { collisionType, nodes ->
            when (collisionType) {
                NodeNameCollisionType.MOVE -> nodeOptionsActionViewModel.moveNodes(nodes)
                NodeNameCollisionType.COPY -> nodeOptionsActionViewModel.copyNodes(nodes)
                else -> { /* No-op for other types */ }
            }
        },
    )
    var visibleNodeOptionId by remember { mutableStateOf(visibleParentNodeOptionId) }
    val nodeOptionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var shouldShowSkeleton by remember { mutableStateOf(false) }
    val isListView = uiState.currentViewType == ViewType.LIST
    val spanCount = rememberDynamicSpanCount(isListView = isListView)
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(visibleParentNodeOptionId) {
        visibleNodeOptionId = visibleParentNodeOptionId
    }

    LaunchedEffect(visibleNodeOptionId) {
        if (visibleNodeOptionId != null) {
            nodeOptionSheetState.show()
        } else {
            nodeOptionSheetState.hide()
        }
    }

    EventEffect(
        event = nodeOptionsActionUiState.downloadEvent,
        onConsumed = nodeOptionsActionViewModel::markDownloadEventConsumed,
        action = onTransfer
    )

    LaunchedEffect(uiState.selectedItemsCount) {
        nodeOptionsActionViewModel.updateSelectionModeAvailableActions(
            uiState.selectedNodes.toSet(),
            NodeSourceType.CLOUD_DRIVE
        )
    }

    // Reset selection mode after handling move, copy, delete action
    LaunchedEffect(nodeActionState.infoToShowEvent) {
        if (nodeActionState.infoToShowEvent is StateEventWithContentTriggered) {
            onAction(CloudDriveAction.DeselectAllItems)
        }
    }

    // Reset selection mode after handling name collision
    LaunchedEffect(nodeActionState.nodeNameCollisionsResult) {
        if (nodeActionState.nodeNameCollisionsResult is StateEventWithContentTriggered) {
            onAction(CloudDriveAction.DeselectAllItems)
        }
    }

    EventEffect(
        event = nodeActionState.renameNodeRequestEvent,
        onConsumed = nodeOptionsActionViewModel::resetRenameNodeRequest,
        action = { nodeId ->
            onRenameNode(nodeId)
        }
    )

    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            delay(200L)
            if (this.isActive) {
                shouldShowSkeleton = true
            }
        } else {
            shouldShowSkeleton = false
        }
    }

    Column(
        modifier = modifier
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        // Storage Over Quota Banner
        val isOverQuotaBannerShow = uiState.storageCapacity != StorageOverQuotaCapacity.DEFAULT
        val topPadding = if (isOverQuotaBannerShow || isTabContent) 12.dp else 0.dp
        if (isOverQuotaBannerShow) {
            StorageOverQuotaBannerM3(
                storageCapacity = uiState.storageCapacity,
                onStorageAlmostFullWarningDismiss = { onAction(CloudDriveAction.StorageAlmostFullWarningDismiss) },
                onUpgradeClicked = {
                    megaNavigator.openUpgradeAccount(context)
                },
            )
        }

        when {
            uiState.isLoading -> {
                if (shouldShowSkeleton) {
                    NodesViewSkeleton(
                        isListView = isListView,
                        spanCount = spanCount,
                        contentPadding = PaddingValues(top = topPadding),
                    )
                }
            }

            uiState.isEmpty -> {
                CloudDriveEmptyView(
                    isRootCloudDrive = uiState.isCloudDriveRoot
                )
            }

            else -> NodesView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                listContentPadding = PaddingValues(
                    top = topPadding,
                    bottom = contentPadding.calculateBottomPadding() + 100.dp
                ),
                listState = listState,
                gridState = gridState,
                spanCount = spanCount,
                items = uiState.items,
                isNextPageLoading = uiState.nodesLoadingState == NodesLoadingState.PartiallyLoaded,
                isHiddenNodesEnabled = uiState.isHiddenNodesEnabled,
                showHiddenNodes = uiState.showHiddenNodes,
                onMenuClicked = { visibleNodeOptionId = it.id },
                onItemClicked = { onAction(ItemClicked(it)) },
                onLongClicked = { onAction(ItemLongClicked(it)) },
                sortConfiguration = uiState.selectedSortConfiguration,
                isListView = isListView,
                onSortOrderClick = { showSortBottomSheet = true },
                onChangeViewTypeClicked = { onAction(ChangeViewTypeClicked) },
                showMediaDiscoveryButton = uiState.hasMediaItems && !uiState.isCloudDriveRoot,
                onEnterMediaDiscoveryClick = {
                    navigationHandler.back()
                    navigationHandler.navigate(
                        MediaDiscoveryNavKey(
                            nodeHandle = uiState.currentFolderId.longValue,
                            nodeName = uiState.title.get(context),
                        )
                    )
                },
                inSelectionMode = uiState.isInSelectionMode,
            )
        }

        EventEffect(
            event = uiState.navigateToFolderEvent,
            onConsumed = { onAction(NavigateToFolderEventConsumed) }
        ) { node ->
            onNavigateToFolder(node.id, node.name)
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
                sortOrder = uiState.selectedSortOrder
            )
        }

        UploadingFiles(
            nameCollisionLauncher = nameCollisionLauncher,
            parentNodeId = uiState.currentFolderId,
            uris = uploadUris,
            onStartUpload = { transferTriggerEvent ->
                onTransfer(transferTriggerEvent)
                uploadUris = emptyList()
            },
        )

        if (showUploadOptionsBottomSheet) {
            UploadOptionsBottomSheet(
                onUploadFilesClicked = {
                    isUploadFolder = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        manualUploadFilesLauncher.launch(POST_NOTIFICATIONS)
                    } else {
                        runCatching {
                            openMultipleDocumentLauncher.launch(arrayOf("*/*"))
                        }.onFailure {
                            Timber.e(it, "Activity not found")
                        }
                    }
                },
                onUploadFolderClicked = {
                    isUploadFolder = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        manualUploadFilesLauncher.launch(POST_NOTIFICATIONS)
                    } else {
                        runCatching {
                            uploadFolderLauncher.launch(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),
                                    null
                                )
                            )
                        }.onFailure {
                            Timber.e(it, "Activity not found")
                        }
                    }
                },
                onScanDocumentClicked = {
                    onAction(CloudDriveAction.StartDocumentScanning)
                },
                onCaptureClicked = {
                    cameraPermissionLauncher.launch(arrayOf(CAMERA, RECORD_AUDIO))
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

        // Todo: We will remove this, and replace it with NavigationHandler
        // Temporary solution to show node options bottom sheet, because navigation file
        // is not yet implemented in node-components module.
        visibleNodeOptionId?.let { nodeId ->
            MegaModalBottomSheet(
                modifier = Modifier.statusBarsPadding(),
                sheetState = nodeOptionSheetState,
                onDismissRequest = {
                    visibleNodeOptionId = null
                    onDismissNodeOptionsBottomSheet()
                },
                bottomSheetBackground = MegaModalBottomSheetBackground.Surface1
            ) {
                NodeOptionsBottomSheetRoute(
                    navigationHandler = navigationHandler,
                    onDismiss = {
                        visibleNodeOptionId = null
                        onDismissNodeOptionsBottomSheet()
                    },
                    nodeId = nodeId.longValue,
                    nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                    onTransfer = onTransfer,
                    actionHandler = nodeActionHandler,
                    nodeOptionsActionViewModel = nodeOptionsActionViewModel,
                )
            }
        }

        if (showSortBottomSheet) {
            SortBottomSheet(
                title = stringResource(sharedR.string.action_sort_by_header),
                options = NodeSortOption.entries,
                sheetState = sortBottomSheetState,
                selectedSort = SortBottomSheetResult(
                    sortOptionItem = uiState.selectedSortConfiguration.sortOption,
                    sortDirection = uiState.selectedSortConfiguration.sortDirection
                ),
                onSortOptionSelected = { result ->
                    result?.let {
                        onSortNodes(
                            NodeSortConfiguration(
                                sortOption = it.sortOptionItem,
                                sortDirection = it.sortDirection
                            )
                        )
                        showSortBottomSheet = false
                    }
                },
                onDismissRequest = {
                    showSortBottomSheet = false
                }
            )
        }
    }
}