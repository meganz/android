package mega.privacy.android.feature.clouddrive.presentation.clouddrive.view

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.dialog.newfolderdialog.NewFolderNodeDialog
import mega.privacy.android.core.nodecomponents.dialog.textfile.NewTextFileNodeDialog
import mega.privacy.android.core.nodecomponents.list.NodesView
import mega.privacy.android.core.nodecomponents.list.NodesViewSkeleton
import mega.privacy.android.core.nodecomponents.list.UnverifiedContactShareBanner
import mega.privacy.android.core.nodecomponents.list.rememberDynamicSpanCount
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheet
import mega.privacy.android.core.nodecomponents.sheet.sort.SortBottomSheetResult
import mega.privacy.android.core.nodecomponents.sheet.upload.UploadOptionsBottomSheet
import mega.privacy.android.core.nodecomponents.upload.UploadingFiles
import mega.privacy.android.core.nodecomponents.upload.rememberCaptureHandler
import mega.privacy.android.core.nodecomponents.upload.rememberUploadHandler
import mega.privacy.android.core.sharedcomponents.extension.excludingBottomPadding
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.pitag.PitagTrigger
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
import mega.privacy.android.feature.transfers.components.OverQuotaBanner
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.MediaDiscoveryNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.navigation.extensions.rememberMegaResultContract
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.CloudDriveChildNodeMoreButtonPressedEvent
import mega.privacy.mobile.analytics.event.CloudDriveEmptyStateAddFilesPressedEvent
import mega.privacy.mobile.analytics.event.SortButtonPressedEvent
import mega.privacy.mobile.analytics.event.SortByDateAddedMenuItemEvent
import mega.privacy.mobile.analytics.event.SortByDateModifiedMenuItemEvent
import mega.privacy.mobile.analytics.event.SortByFavouriteMenuItemEvent
import mega.privacy.mobile.analytics.event.SortByLabelMenuItemEvent
import mega.privacy.mobile.analytics.event.SortByLinkCreationMenuItemEvent
import mega.privacy.mobile.analytics.event.SortByNameMenuItemEvent
import mega.privacy.mobile.analytics.event.SortByShareCreationMenuItemEvent
import mega.privacy.mobile.analytics.event.SortBySizeMenuItemEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CloudDriveContent(
    isTabContent: Boolean,
    navigationHandler: NavigationHandler,
    uiState: CloudDriveUiState,
    showUploadOptionsBottomSheet: Boolean,
    onToggleShowUploadOptionsBottomSheet: (Boolean) -> Unit,
    onAction: (CloudDriveAction) -> Unit,
    onNavigateBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onSortNodes: (NodeSortConfiguration) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp, 0.dp),
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    nodeOptionsActionViewModel: NodeOptionsActionViewModel = hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
        creationCallback = { it.create(NodeSourceType.CLOUD_DRIVE) }
    ),
    onPrepareScanDocument: () -> Unit = {},
) {
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewTextFileDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackBarHostState.current
    val megaResultContract = rememberMegaResultContract()
    val megaNavigator = rememberMegaNavigator()
    var pitagTrigger by rememberSaveable { mutableStateOf(PitagTrigger.NotApplicable) }
    var uploadUris by rememberSaveable { mutableStateOf(emptyList<Uri>()) }
    val nodeActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()

    val uploadHandler = rememberUploadHandler(
        parentId = uiState.currentFolderId,
        onFilesSelected = { uris ->
            pitagTrigger = PitagTrigger.Picker
            uploadUris = uris
        },
        megaNavigator = megaNavigator,
        megaResultContract = megaResultContract
    )

    val captureHandler = rememberCaptureHandler(
        onPhotoCaptured = { uri ->
            pitagTrigger = PitagTrigger.CameraCapture
            uploadUris = listOf(uri)
        },
        megaResultContract = megaResultContract
    )

    val nameCollisionLauncher = rememberLauncherForActivityResult(
        contract = megaResultContract.nameCollisionActivityContract
    ) { message ->
        if (!message.isNullOrEmpty()) {
            coroutineScope.launch {
                snackbarHostState?.showAutoDurationSnackbar(message)
            }
        }
    }
    var shouldShowSkeleton by remember { mutableStateOf(false) }
    val isListView = uiState.currentViewType == ViewType.LIST
    val spanCount = rememberDynamicSpanCount(isListView = isListView)
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.selectedItemsCount) {
        nodeOptionsActionViewModel.updateSelectionModeAvailableActions(
            uiState.selectedNodes.toSet(),
            nodeSourceType = uiState.nodeSourceType
        )
    }

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

    EventEffect(
        event = nodeActionState.actionTriggeredEvent,
        onConsumed = nodeOptionsActionViewModel::resetActionTriggered
    ) {
        onAction(CloudDriveAction.DeselectAllItems)
    }


    EventEffect(
        event = nodeActionState.dismissEvent,
        onConsumed = nodeOptionsActionViewModel::resetDismiss
    ) {
        onAction(CloudDriveAction.DeselectAllItems)
    }

    Column(
        modifier = modifier
            .padding(contentPadding.excludingBottomPadding())
    ) {
        val showQuotaBanner = uiState.isStorageOverQuota || uiState.isTransferOverQuota
        val showContactVerificationBanner =
            !uiState.isLoading && uiState.showContactNotVerifiedBanner && uiState.title.text.isNotEmpty()
        val topPadding =
            if (showQuotaBanner || isTabContent || showContactVerificationBanner) 12.dp else 0.dp

        if (showQuotaBanner) {
            OverQuotaBanner(
                isStorageOverQuota = uiState.isStorageOverQuota,
                isTransferOverQuota = uiState.isTransferOverQuota,
                onUpgradeClick = {
                    megaNavigator.openUpgradeAccount(context)
                },
                onCancelButtonClick = {
                    onAction(CloudDriveAction.OverQuotaConsumptionWarning)
                }
            )
        }

        if (showContactVerificationBanner) {
            UnverifiedContactShareBanner(
                text = stringResource(
                    id = R.string.contact_incoming_shared_folder_contact_not_approved_alert_text,
                    uiState.title.text
                ),
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
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 56.dp),
                    isRootCloudDrive = uiState.isCloudDriveRoot,
                    onAddItemsClicked = {
                        Analytics.tracker.trackEvent(CloudDriveEmptyStateAddFilesPressedEvent)
                        onToggleShowUploadOptionsBottomSheet(true)
                    },
                    showAddItems = uiState.hasWritePermission
                )
            }

            else -> NodesView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                listContentPadding = PaddingValues(
                    top = topPadding,
                    bottom = contentPadding.calculateBottomPadding() + 100.dp,
                ),
                listState = listState,
                gridState = gridState,
                spanCount = spanCount,
                items = uiState.items,
                isNextPageLoading = uiState.nodesLoadingState == NodesLoadingState.PartiallyLoaded,
                isHiddenNodesEnabled = uiState.isHiddenNodesEnabled,
                showHiddenNodes = uiState.showHiddenNodes,
                onMenuClicked = {
                    Analytics.tracker.trackEvent(CloudDriveChildNodeMoreButtonPressedEvent)
                    navigationHandler.navigate(
                        NodeOptionsBottomSheetNavKey(
                            nodeHandle = it.id.longValue,
                            nodeSourceType = uiState.nodeSourceType,
                        )
                    )
                },
                onItemClicked = { onAction(ItemClicked(it)) },
                onLongClicked = { onAction(ItemLongClicked(it)) },
                sortConfiguration = uiState.selectedSortConfiguration,
                isListView = isListView,
                onSortOrderClick = {
                    Analytics.tracker.trackEvent(SortButtonPressedEvent)
                    showSortBottomSheet = true
                },
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
                isContactVerificationOn = uiState.isContactVerificationOn
            )
        }

        EventEffect(
            event = uiState.navigateToFolderEvent,
            onConsumed = { onAction(NavigateToFolderEventConsumed) }
        ) { node ->
            navigationHandler.navigate(
                CloudDriveNavKey(
                    nodeHandle = node.id.longValue,
                    nodeName = node.name
                )
            )
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
                nodeSourceType = uiState.nodeSourceType,
                onDownloadEvent = onTransfer,
                sortOrder = uiState.selectedSortOrder,
                onNavigate = navigationHandler::navigate,
            )
        }

        UploadingFiles(
            nameCollisionLauncher = nameCollisionLauncher,
            parentNodeId = uiState.currentFolderId,
            uris = uploadUris,
            pitagTrigger = pitagTrigger,
            onStartUpload = { transferTriggerEvent ->
                onTransfer(transferTriggerEvent)
                pitagTrigger = PitagTrigger.NotApplicable
                uploadUris = emptyList()
            },
        )

        if (showUploadOptionsBottomSheet) {
            UploadOptionsBottomSheet(
                onUploadFilesClicked = {
                    uploadHandler.onUploadFilesClicked()
                },
                onUploadFolderClicked = {
                    uploadHandler.onUploadFolderClicked()
                },
                onScanDocumentClicked = {
                    onPrepareScanDocument()
                },
                onCaptureClicked = {
                    captureHandler.onCaptureClicked()
                },
                onNewFolderClicked = {
                    showNewFolderDialog = true
                },
                onNewTextFileClicked = {
                    showNewTextFileDialog = true
                },
                onDismissSheet = {
                    onToggleShowUploadOptionsBottomSheet(false)
                }
            )
        }

        if (showNewFolderDialog) {
            NewFolderNodeDialog(
                parentNode = uiState.currentFolderId,
                onCreateFolder = { folderId ->
                    showNewFolderDialog = false
                    coroutineScope.launch {
                        if (folderId != null) {
                            navigationHandler.navigate(
                                CloudDriveNavKey(
                                    nodeHandle = folderId.longValue,
                                    isNewFolder = true
                                )
                            )
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

        if (showSortBottomSheet) {
            SortBottomSheet(
                title = stringResource(sharedR.string.action_sort_by_header),
                options = NodeSortOption.getOptionsForSourceType(uiState.nodeSourceType),
                sheetState = sortBottomSheetState,
                selectedSort = SortBottomSheetResult(
                    sortOptionItem = uiState.selectedSortConfiguration.sortOption,
                    sortDirection = uiState.selectedSortConfiguration.sortDirection
                ),
                onSortOptionSelected = { result ->
                    result?.let {
                        it.sortOptionItem.trackAnalyticsEvent()
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

private fun NodeSortOption.trackAnalyticsEvent() {
    val event = when (this) {
        NodeSortOption.Name -> SortByNameMenuItemEvent
        NodeSortOption.Favourite -> SortByFavouriteMenuItemEvent
        NodeSortOption.Label -> SortByLabelMenuItemEvent
        NodeSortOption.Created -> SortByDateAddedMenuItemEvent
        NodeSortOption.Modified -> SortByDateModifiedMenuItemEvent
        NodeSortOption.Size -> SortBySizeMenuItemEvent
        NodeSortOption.ShareCreated -> SortByShareCreationMenuItemEvent
        NodeSortOption.LinkCreated -> SortByLinkCreationMenuItemEvent
    }

    Analytics.tracker.trackEvent(event)
}
