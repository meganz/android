package mega.privacy.android.feature.clouddrive.presentation.folderbrowser.view

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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.modifiers.applyScrollToHideBehavior
import mega.android.core.ui.modifiers.calculateSafeBottomPadding
import mega.android.core.ui.modifiers.excludingBottomPadding
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.HandleNodeAction3
import mega.privacy.android.core.nodecomponents.action.NodeSourceData
import mega.privacy.android.core.nodecomponents.list.UnverifiedContactShareBanner
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.core.nodecomponents.sheet.upload.UploadOptionsBottomSheet
import mega.privacy.android.core.nodecomponents.upload.rememberCaptureHandler
import mega.privacy.android.core.nodecomponents.upload.rememberUploadHandler
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.CloudDriveEmptyView
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.sortOptionsSourceType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.trackAnalyticsEvent
import mega.privacy.android.feature.clouddrive.presentation.folderbrowser.model.FolderBrowserAction
import mega.privacy.android.feature.clouddrive.presentation.folderbrowser.model.FolderBrowserUiState
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CloudDriveMediaDiscoveryNavKey
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.NewFolderDialogNavKey
import mega.privacy.android.navigation.destination.NewTextFileDialogNavKey
import mega.privacy.android.navigation.destination.OpenLinkDialogNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.navigation.extensions.rememberMegaResultContract
import mega.privacy.android.shared.account.inactivity.view.AccountInactivityBanner
import mega.privacy.android.shared.account.overquota.OverQuotaStatusViewModel
import mega.privacy.android.shared.account.overquota.model.OverQuotaIssue
import mega.privacy.android.shared.account.overquota.view.OverQuotaErrorBanner
import mega.privacy.android.shared.account.overquota.view.OverQuotaWarningBanner
import mega.privacy.android.shared.nodes.components.NodeSkeletons
import mega.privacy.android.shared.nodes.components.NodesView
import mega.privacy.android.shared.nodes.components.NodesViewSkeleton
import mega.privacy.android.shared.nodes.components.ScrollToHideNodeHeaderItem
import mega.privacy.android.shared.nodes.components.SortBottomSheet
import mega.privacy.android.shared.nodes.components.SortBottomSheetResult
import mega.privacy.android.shared.nodes.components.rememberDynamicSpanCount
import mega.privacy.android.shared.nodes.dialog.newfolder.NewFolderResultEffect
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.nodes.namecollision.rememberNameCollisionLauncher
import mega.privacy.android.shared.nodes.selection.NodeSelection
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.transfers.components.UploadingFiles
import mega.privacy.android.shared.transfers.components.rememberUploadUrisEventState
import mega.privacy.mobile.analytics.event.CloudDriveChildNodeMoreButtonPressedEvent
import mega.privacy.mobile.analytics.event.CloudDriveEmptyStateAddFilesPressedEvent
import mega.privacy.mobile.analytics.event.SortButtonPressedEvent

/**
 * The paged folder content: banners, the node list fed by the pager, upload flows and sheets.
 * A copy of `CloudDriveContent` for the paged stack; a fix to either during the flag window must
 * be mirrored in the other.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FolderBrowserContent(
    isTabContent: Boolean,
    navigationHandler: NavigationHandler,
    uiState: FolderBrowserUiState,
    showUploadOptionsBottomSheet: Boolean,
    onToggleShowUploadOptionsBottomSheet: (Boolean) -> Unit,
    onAction: (FolderBrowserAction) -> Unit,
    onNavigateBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onSortNodes: (NodeSortConfiguration) -> Unit,
    selection: NodeSelection<TypedNode>,
    navigateToFolder: (TypedFolderNode, NodeSourceType) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp, 0.dp),
    isActive: Boolean = true,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    onPrepareScanDocument: () -> Unit = {},
    overQuotaStatusViewModel: OverQuotaStatusViewModel = hiltViewModel(),
) {
    val overQuotaUiState by overQuotaStatusViewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackBarHostState.current
    val megaResultContract = rememberMegaResultContract()
    val megaNavigator = rememberMegaNavigator()
    var pitagTrigger by rememberSaveable { mutableStateOf(PitagTrigger.NotApplicable) }
    val uploadUrisEventState = rememberUploadUrisEventState()
    val captureHandler = rememberCaptureHandler(
        onPhotoCaptured = { uri ->
            pitagTrigger = PitagTrigger.CameraCapture
            uploadUrisEventState.trigger(listOf(uri))
        },
        megaResultContract = megaResultContract
    )

    val nameCollisionLauncher = rememberNameCollisionLauncher(navigate = navigationHandler::navigate)
    val isListView = uiState.currentViewType == ViewType.LIST
    val spanCount = rememberDynamicSpanCount(isListView = isListView)
    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(contentPadding.excludingBottomPadding())
    ) {
        OverQuotaErrorBanner(
            overQuotaStatus = overQuotaUiState.overQuotaStatus,
            onUpgradeClicked = {
                megaNavigator.openUpgradeAccount(context)
            },
        )
        val folderId = uiState.currentFolderId
        val showContactVerificationBanner =
            uiState.showContactNotVerifiedBanner && uiState.title.text.isNotEmpty()
        val showOverQuotaWarning = !uiState.isLoading &&
                overQuotaUiState.overQuotaStatus.severity is OverQuotaIssue.Severity.Warning &&
                overQuotaUiState.shouldShowWarning

        if (showContactVerificationBanner) {
            UnverifiedContactShareBanner(
                text = stringResource(
                    id = sharedR.string.incoming_shared_folder_contact_not_approved_alert_text,
                    uiState.title.text
                ),
            )
        }

        AccountInactivityBanner(
            enabled = !selection.isInSelectionMode,
            onLearnMoreClicked = { url -> megaNavigator.launchUrl(context, url) },
        )

        if (uiState.isLoading || folderId == null) {
            NodesViewSkeleton(
                isListView = isListView,
                spanCount = spanCount,
                delay = NodeSkeletons.defaultDelay,
            )
        } else {
            if (uiState.items.isEmpty()) {
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
                    showAddItems = uiState.isUploadAllowed
                )
            } else {
                val folderName = uiState.title.text
                var selectedFile: TypedFileNode? by remember { mutableStateOf(null) }
                val onSortOrderClick = {
                    Analytics.tracker.trackEvent(SortButtonPressedEvent)
                    showSortBottomSheet = true
                }
                val onChangeViewTypeClick = {
                    onAction(
                        FolderBrowserAction.ChangeViewTypeClicked(
                            when (uiState.currentViewType) {
                                ViewType.LIST -> ViewType.GRID
                                ViewType.GRID -> ViewType.LIST
                            }
                        )
                    )
                }
                val onEnterMediaDiscoveryClick = {
                    navigationHandler.navigate(
                        CloudDriveMediaDiscoveryNavKey(
                            folderId = folderId.longValue,
                            folderName = folderName,
                            fromFolderLink = false,
                            nodeSourceType = uiState.nodeSourceType,
                        )
                    )
                }

                if (showOverQuotaWarning) {
                    OverQuotaWarningBanner(
                        overQuotaUiState.overQuotaStatus,
                        onDismissed = { overQuotaStatusViewModel.dismissWarning() },
                        onUpgradeClicked = { megaNavigator.openUpgradeAccount(context) },
                        modifier = Modifier.applyScrollToHideBehavior(contributesHeight = isActive),
                    )
                }

                ScrollToHideNodeHeaderItem(
                    isActive = isActive,
                    onSortOrderClick = onSortOrderClick,
                    onChangeViewTypeClick = onChangeViewTypeClick,
                    onEnterMediaDiscoveryClick = onEnterMediaDiscoveryClick,
                    sortConfiguration = uiState.selectedSortConfiguration,
                    isListView = isListView,
                    showMediaDiscoveryButton = uiState.isMediaDiscoveryAllowed,
                )

                NodesView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    listContentPadding = PaddingValues(bottom = contentPadding.calculateSafeBottomPadding()),
                    listState = listState,
                    gridState = gridState,
                    spanCount = spanCount,
                    items = uiState.items,
                    isNextPageLoading = uiState.isNextPageLoading,
                    onMenuClicked = {
                        Analytics.tracker.trackEvent(CloudDriveChildNodeMoreButtonPressedEvent)
                        navigationHandler.navigate(
                            NodeOptionsBottomSheetNavKey(
                                nodeHandle = it.id.longValue,
                                nodeSourceType = uiState.nodeSourceType,
                            )
                        )
                    },
                    onItemClicked = {
                        if (selection.isInSelectionMode) {
                            selection.toggle(it.id)
                        } else {
                            if (it is TypedFileNode) {
                                selectedFile = it
                            } else if (it is TypedFolderNode) {
                                navigateToFolder(it, uiState.nodeSourceType)
                            }
                        }
                    },
                    onLongClicked = { selection.toggle(it.id) },
                    sortConfiguration = uiState.selectedSortConfiguration,
                    isListView = isListView,
                    showSortOrder = false,
                    showChangeViewType = false,
                    nodeSelectionState = selection.state,
                    onVisibleRangeChanged = { onAction(FolderBrowserAction.VisibleRangeChanged(it)) },
                )

                selectedFile?.let { openedFileNode ->
                    HandleNodeAction3(
                        typedFileNode = openedFileNode,
                        snackBarHostState = snackbarHostState,
                        coroutineScope = coroutineScope,
                        onActionHandled = { selectedFile = null },
                        nodeSourceData = NodeSourceData.Default(uiState.nodeSourceType),
                        onDownloadEvent = onTransfer,
                        sortOrder = uiState.selectedSortOrder,
                        onNavigate = navigationHandler::navigate,
                    )
                }
            }

            EventEffect(
                event = uiState.navigateBack,
                onConsumed = { onAction(FolderBrowserAction.NavigateBackEventConsumed) }
            ) {
                onNavigateBack()
            }

            UploadingFiles(
                onNameCollisions = nameCollisionLauncher::launch,
                parentNodeId = folderId,
                urisEvent = uploadUrisEventState.event,
                onUrisConsumed = uploadUrisEventState::consume,
                pitagTrigger = pitagTrigger,
                onStartUpload = { transferTriggerEvent ->
                    onTransfer(transferTriggerEvent)
                    pitagTrigger = PitagTrigger.NotApplicable
                },
            )

            val uploadHandler = rememberUploadHandler(
                parentId = folderId,
                onFilesSelected = { uris ->
                    pitagTrigger = PitagTrigger.Picker
                    uploadUrisEventState.trigger(uris)
                },
                megaNavigator = megaNavigator,
                megaResultContract = megaResultContract
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
                        navigationHandler.navigate(
                            NewFolderDialogNavKey(parentNodeId = folderId)
                        )
                    },
                    onNewTextFileClicked = {
                        navigationHandler.navigate(
                            NewTextFileDialogNavKey(parentNodeId = folderId)
                        )
                    },
                    onOpenLinkClicked = {
                        navigationHandler.navigate(OpenLinkDialogNavKey())
                    },
                    onDismissSheet = {
                        onToggleShowUploadOptionsBottomSheet(false)
                    },
                )
            }

            if (showSortBottomSheet) {
                SortBottomSheet(
                    title = stringResource(sharedR.string.action_sort_by_header),
                    options = NodeSortOption.getOptionsForSourceType(uiState.nodeSourceType.sortOptionsSourceType()),
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

    NewFolderResultEffect(
        monitorResult = navigationHandler::monitorResult,
        clearResult = navigationHandler::clearResult,
        onFolderCreated = { createdFolderId ->
            navigationHandler.navigate(
                CloudDriveNavKey(
                    nodeHandle = createdFolderId.longValue,
                    nodeSourceType = uiState.nodeSourceType
                )
            )
        },
    )
}
