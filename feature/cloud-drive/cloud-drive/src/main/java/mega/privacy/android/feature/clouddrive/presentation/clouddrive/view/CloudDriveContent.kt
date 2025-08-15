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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.dialog.newfolderdialog.NewFolderNodeDialog
import mega.privacy.android.core.nodecomponents.dialog.textfile.NewTextFileNodeDialog
import mega.privacy.android.core.nodecomponents.list.NodesView
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.nodecomponents.sheet.upload.UploadOptionsBottomSheet
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
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
import mega.privacy.android.feature.clouddrive.presentation.upload.UploadingFiles
import mega.privacy.android.navigation.ExtraConstant
import mega.privacy.android.navigation.camera.CameraArg
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.navigation.extensions.rememberMegaResultContract
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CloudDriveContent(
    uiState: CloudDriveUiState,
    showUploadOptionsBottomSheet: Boolean,
    onDismissUploadOptionsBottomSheet: () -> Unit,
    onAction: (CloudDriveAction) -> Unit,
    onNavigateToFolder: (NodeId, String?) -> Unit,
    onNavigateBack: () -> Unit,
    onCreatedNewFolder: (NodeId) -> Unit,
    openNodeOptions: (NodeId) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
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
    val megaResultContract = rememberMegaResultContract()
    val megaNavigator = rememberMegaNavigator()
    var uploadUris by rememberSaveable { mutableStateOf(emptyList<Uri>()) }
    var isUploadFolder by rememberSaveable { mutableStateOf(false) }

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
    var visibleNodeOptionId by remember { mutableStateOf<NodeId?>(null) }
    var nodeOptionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var shouldShowSkeleton by remember { mutableStateOf(false) }

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

    when {
        uiState.isLoading -> {
            if (shouldShowSkeleton) {
                NodesViewSkeleton(
                    contentPadding = contentPadding,
                    isListView = uiState.currentViewType == ViewType.LIST
                )
            }
        }

        uiState.isEmpty -> {
            CloudDriveEmptyView(
                isRootCloudDrive = uiState.isCloudDriveRoot
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
            onMenuClick = { openNodeOptions(it.node.id) },
            onItemClicked = { onAction(ItemClicked(it)) },
            onLongClicked = { onAction(ItemLongClicked(it)) },
            sortOrder = "Name", // TODO: Replace with actual sort order from state
            isListView = uiState.currentViewType == ViewType.LIST,
            onSortOrderClick = {}, // TODO: Handle sort order click
            onChangeViewTypeClicked = { onAction(ChangeViewTypeClicked) },
            showMediaDiscoveryButton = false,
            onEnterMediaDiscoveryClick = {}, // TODO: Handle media discovery click
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
            sortOrder = SortOrder.ORDER_NONE
        )
    }

    UploadingFiles(
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
}