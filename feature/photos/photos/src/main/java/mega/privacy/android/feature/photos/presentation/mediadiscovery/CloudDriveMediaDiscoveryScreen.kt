package mega.privacy.android.feature.photos.presentation.mediadiscovery

import MediaGridViewItem
import android.annotation.SuppressLint
import android.net.Uri
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.dropdown.DropDownItem
import mega.android.core.ui.components.dropdown.MegaDropDownMenu
import mega.android.core.ui.components.scrollbar.fastscroll.FastScrollLazyVerticalGrid
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.android.core.ui.modifiers.applyScrollToHideFabBehavior
import mega.android.core.ui.modifiers.conditional
import mega.android.core.ui.modifiers.excludingBottomPadding
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.MultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.AddContentFab
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.dialog.newfolderdialog.NewFolderNodeDialog
import mega.privacy.android.core.nodecomponents.dialog.textfile.NewTextFileNodeDialog
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.core.nodecomponents.sheet.upload.UploadOptionsBottomSheet
import mega.privacy.android.core.nodecomponents.upload.ScanDocumentHandler
import mega.privacy.android.core.nodecomponents.upload.ScanDocumentViewModel
import mega.privacy.android.core.nodecomponents.upload.UploadingFiles
import mega.privacy.android.core.nodecomponents.upload.rememberUploadUrisEventState
import mega.privacy.android.core.nodecomponents.upload.rememberCaptureHandler
import mega.privacy.android.core.nodecomponents.upload.rememberUploadHandler
import mega.privacy.android.core.sharedcomponents.menu.CommonAppBarAction
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.entity.photos.DateCard
import mega.privacy.android.domain.entity.photos.FilterMediaType
import mega.privacy.android.domain.entity.photos.MediaListItem
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.Sort
import mega.privacy.android.domain.entity.photos.ZoomLevel
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.extensions.photosZoomGestureDetector
import mega.privacy.android.feature.photos.presentation.mediadiscovery.component.MediaDiscoveryFilterDialog
import mega.privacy.android.feature.photos.presentation.mediadiscovery.component.MediaDiscoverySortDialog
import mega.privacy.android.feature.photos.presentation.mediadiscovery.model.MediaDiscoveryPeriod
import mega.privacy.android.feature.photos.presentation.mediadiscovery.view.MediaDiscoveryCardListView
import mega.privacy.android.feature.photos.presentation.mediadiscovery.view.MediaDiscoveryLoadingView
import mega.privacy.android.feature.photos.presentation.mediadiscovery.view.MediaDiscoveryPeriodChip
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.LegacyImageViewerNavKey
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.navigation.extensions.rememberMegaResultContract
import mega.privacy.android.shared.nodes.components.NodeHeaderItem
import mega.privacy.android.shared.nodes.components.NodeSelectionModeAppBar
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.CloudDriveFABPressedEvent
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@Composable
fun CloudDriveMediaDiscoveryRoute(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CloudDriveMediaDiscoveryViewModel = hiltViewModel<CloudDriveMediaDiscoveryViewModel, CloudDriveMediaDiscoveryViewModel.Factory>(
        creationCallback = { it.create() }
    ),
    nodeOptionsActionViewModel: NodeOptionsActionViewModel =
        hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
            creationCallback = { it.create(null) }
        ),
    scanDocumentViewModel: ScanDocumentViewModel = hiltViewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onMoreOptionsClicked: () -> Unit = {},
) {
    val multiNodeActionHandler = rememberMultiNodeActionHandler(
        viewModel = nodeOptionsActionViewModel,
        navigationHandler = navigationHandler
    )
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val actionUiState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val megaNavigator = rememberMegaNavigator()
    val megaResultContract = rememberMegaResultContract()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackBarHostState.current

    var showUploadOptionsBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showNewFolderDialog by rememberSaveable { mutableStateOf(false) }
    var showNewTextFileDialog by rememberSaveable { mutableStateOf(false) }
    var pitagTrigger by rememberSaveable { mutableStateOf(PitagTrigger.NotApplicable) }
    val uploadUrisEventState = rememberUploadUrisEventState()
    val parentId = NodeId(viewModel.folderId)
    val uploadHandler = rememberUploadHandler(
        parentId = parentId,
        onFilesSelected = { uris ->
            pitagTrigger = PitagTrigger.Picker
            uploadUrisEventState.trigger(uris)
        },
        megaNavigator = megaNavigator,
        megaResultContract = megaResultContract
    )

    val captureHandler = rememberCaptureHandler(
        onPhotoCaptured = { uri ->
            pitagTrigger = PitagTrigger.CameraCapture
            uploadUrisEventState.trigger(listOf(uri))
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

    LaunchedEffect(uiState.selectedPhotosCount) {
        nodeOptionsActionViewModel.updateSelectionModeAvailableActions(
            selectedNodes = uiState.selectedNodes,
            nodeSourceType = uiState.nodeSourceType
        )
    }

    EventEffect(
        event = actionUiState.actionTriggeredEvent,
        onConsumed = nodeOptionsActionViewModel::resetActionTriggered
    ) {
        viewModel.clearSelectedPhotos()
    }

    EventEffect(
        event = actionUiState.dismissEvent,
        onConsumed = nodeOptionsActionViewModel::resetDismiss,
    ) {
        viewModel.clearSelectedPhotos()
    }

    CloudDriveMediaDiscoveryScreen(
        uiState = uiState,
        actionUiState = actionUiState,
        multiNodeActionHandler = multiNodeActionHandler,
        showUploadOptionsBottomSheet = showUploadOptionsBottomSheet,
        onBack = navigationHandler::back,
        onItemClicked = { photo ->
            if (uiState.isInSelectionMode) {
                viewModel.selectPhoto(photo)
            } else {
                navigationHandler.navigate(
                    LegacyImageViewerNavKey(
                        nodeHandle = photo.id,
                        parentNodeHandle = -1L,
                        nodeIds = uiState.sourcePhotos.map { it.id },
                    )
                )
            }
        },
        onItemLongPressed = viewModel::selectPhoto,
        onPeriodSelected = viewModel::updatePeriod,
        selectAllItems = viewModel::selectAllPhotos,
        deselectAllItems = viewModel::clearSelectedPhotos,
        onCardClick = viewModel::selectPeriod,
        onMoreOptionsClicked = onMoreOptionsClicked,
        onFabClicked = {
            Analytics.tracker.trackEvent(CloudDriveFABPressedEvent)
            showUploadOptionsBottomSheet = true
        },
        onUploadFilesClicked = { uploadHandler.onUploadFilesClicked() },
        onUploadFolderClicked = { uploadHandler.onUploadFolderClicked() },
        onScanDocumentClicked = { scanDocumentViewModel.prepareDocumentScanner() },
        onCaptureClicked = { captureHandler.onCaptureClicked() },
        onNewFolderClicked = { showNewFolderDialog = true },
        onNewTextFileClicked = { showNewTextFileDialog = true },
        onDismissUploadSheet = { showUploadOptionsBottomSheet = false },
        onTransfersClicked = { navigationHandler.navigate(TransfersNavKey()) },
        onClickZoomIn = viewModel::zoomIn,
        onClickZoomOut = viewModel::zoomOut,
        onSetCurrentSort = viewModel::setCurrentSort,
        onSetCurrentMediaType = viewModel::setCurrentMediaType,
        modifier = modifier,
        contentPadding = contentPadding,
    )

    BackHandler(enabled = uiState.isInSelectionMode) {
        viewModel.clearSelectedPhotos()
    }

    UploadingFiles(
        nameCollisionLauncher = nameCollisionLauncher,
        parentNodeId = parentId,
        urisEvent = uploadUrisEventState.event,
        onUrisConsumed = uploadUrisEventState::consume,
        pitagTrigger = pitagTrigger,
        onStartUpload = { transferTriggerEvent ->
            onTransfer(transferTriggerEvent)
            pitagTrigger = PitagTrigger.NotApplicable
        },
    )

    if (showNewFolderDialog) {
        NewFolderNodeDialog(
            parentNode = parentId,
            onCreateFolder = { folderId ->
                showNewFolderDialog = false
                if (folderId != null) {
                    navigationHandler.navigate(
                        CloudDriveNavKey(
                            nodeHandle = folderId.longValue,
                            nodeSourceType = uiState.nodeSourceType
                        )
                    )
                }
            },
            onDismiss = { showNewFolderDialog = false }
        )
    }

    if (showNewTextFileDialog) {
        NewTextFileNodeDialog(
            parentNode = parentId,
            onDismiss = { showNewTextFileDialog = false }
        )
    }

    @SuppressLint("ComposeViewModelForwarding")
    ScanDocumentHandler(
        parentNodeId = parentId,
        viewModel = scanDocumentViewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CloudDriveMediaDiscoveryScreen(
    uiState: CloudDriveMediaDiscoveryUiState,
    actionUiState: NodeActionState,
    multiNodeActionHandler: MultiNodeActionHandler,
    showUploadOptionsBottomSheet: Boolean,
    onBack: () -> Unit,
    onItemClicked: (Photo) -> Unit,
    onItemLongPressed: (Photo) -> Unit,
    selectAllItems: () -> Unit,
    deselectAllItems: () -> Unit,
    onMoreOptionsClicked: () -> Unit,
    onPeriodSelected: (MediaDiscoveryPeriod) -> Unit,
    onCardClick: (DateCard) -> Unit,
    onFabClicked: () -> Unit,
    onUploadFilesClicked: () -> Unit,
    onUploadFolderClicked: () -> Unit,
    onScanDocumentClicked: () -> Unit,
    onCaptureClicked: () -> Unit,
    onNewFolderClicked: () -> Unit,
    onNewTextFileClicked: () -> Unit,
    onDismissUploadSheet: () -> Unit,
    onTransfersClicked: () -> Unit,
    onClickZoomIn: () -> Unit,
    onClickZoomOut: () -> Unit,
    onSetCurrentSort: (Sort) -> Unit,
    onSetCurrentMediaType: (FilterMediaType) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var isPeriodVisible by remember { mutableStateOf(true) }
    var isDropdownVisible by remember { mutableStateOf(false) }
    var showSortByDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            AddContentFab(
                modifier = Modifier
                    .testTag(MEDIA_DISCOVERY_FAB_TAG)
                    .applyScrollToHideFabBehavior(),
                visible = uiState.isUploadAllowed && uiState.loadPhotosDone,
                onClick = onFabClicked
            )
        },
        topBar = {
            if (uiState.isInSelectionMode) {
                NodeSelectionModeAppBar(
                    count = uiState.selectedPhotosCount,
                    isAllSelected = uiState.isAllSelected,
                    isSelecting = false,
                    onSelectAllClicked = selectAllItems,
                    onCancelSelectionClicked = deselectAllItems
                )
            } else {
                MegaTopAppBar(
                    title = uiState.folderName,
                    navigationType = AppBarNavigationType.Back(onBack),
                    trailingIcons = {
                        TransfersToolbarWidget(
                            onClick = {
                                onTransfersClicked()
                            }
                        )
                    },
                    actions = listOf(
                        MenuActionWithClick(
                            menuAction = object : MenuActionWithIcon {
                                @Composable
                                override fun getIconPainter(): Painter =
                                    rememberVectorPainter(IconPack.Medium.Thin.Outline.SlidersVertical02)

                                override val testTag: String =
                                    "CloudDriveMediaDiscoveryScreen:top_app_bar_filter"

                                @Composable
                                override fun getDescription(): String =
                                    stringResource(sharedR.string.general_action_filter)
                            },
                            onClick = {
                                isDropdownVisible = true
                            }
                        ),
                        MenuActionWithClick(CommonAppBarAction.More) {
                            onMoreOptionsClicked()
                        }
                    )
                )
            }
        },
        bottomBar = {
            NodeSelectionModeBottomBar(
                availableActions = actionUiState.availableActions,
                visibleActions = actionUiState.visibleActions,
                visible = actionUiState.visibleActions.isNotEmpty() && uiState.isInSelectionMode,
                multiNodeActionHandler = multiNodeActionHandler,
                selectedNodes = uiState.selectedNodes.toList(),
                isSelecting = false,
            )
        }
    ) { containerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(containerPadding.excludingBottomPadding())
        ) {
            val hasPhotos = uiState.sourcePhotos.isNotEmpty()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.TopEnd)
                    .padding(end = 58.dp)
            ) {
                MegaDropDownMenu(
                    modifier = Modifier.width(173.dp),
                    expanded = isDropdownVisible,
                    onDismissRequest = { isDropdownVisible = false },
                    onItemClick = { item ->
                        isDropdownVisible = false
                        when (item.id) {
                            MediaDiscoveryDropDownAction.Filter.ordinal -> showFilterDialog = true
                            MediaDiscoveryDropDownAction.SortBy.ordinal -> showSortByDialog = true
                            MediaDiscoveryDropDownAction.ZoomIn.ordinal -> onClickZoomIn()
                            MediaDiscoveryDropDownAction.ZoomOut.ordinal -> onClickZoomOut()
                        }
                    },
                    dropdownItems = MediaDiscoveryDropDownAction.entries.map { action ->
                        DropDownItem(
                            id = action.ordinal,
                            text = stringResource(action.textRes),
                            enabled = when (action) {
                                MediaDiscoveryDropDownAction.Filter -> true
                                MediaDiscoveryDropDownAction.SortBy -> hasPhotos
                                MediaDiscoveryDropDownAction.ZoomIn ->
                                    uiState.currentZoomLevel != ZoomLevel.Grid_1 && hasPhotos

                                MediaDiscoveryDropDownAction.ZoomOut ->
                                    uiState.currentZoomLevel != ZoomLevel.Grid_5 && hasPhotos
                            },
                        )
                    },
                )
            }

            when {
                !uiState.loadPhotosDone -> {
                    MediaDiscoveryLoadingView(
                        modifier = Modifier.fillMaxSize(),
                        onChangeViewType = onBack
                    )
                }

                uiState.mediaListItemList.isEmpty() -> {
                    EmptyStateView(
                        modifier = Modifier.fillMaxSize(),
                        illustration = R.drawable.il_glass_image,
                        description = SpannableText(
                            text = stringResource(sharedR.string.timeline_tab_empty_body_no_media_found)
                        )
                    )
                }

                else -> {
                    when (uiState.selectedPeriod) {
                        MediaDiscoveryPeriod.All -> {
                            MediaDiscoveryGridView(
                                uiState = uiState,
                                onBack = onBack,
                                contentPadding = contentPadding,
                                onScrollingListener = { isScrolling ->
                                    isPeriodVisible = !isScrolling
                                },
                                onItemClicked = onItemClicked,
                                onItemLongPressed = onItemLongPressed,
                                zoomIn = onClickZoomIn,
                                zoomOut = onClickZoomOut,
                            )
                        }

                        else -> {
                            val dateCards = when (uiState.selectedPeriod) {
                                MediaDiscoveryPeriod.Years -> uiState.yearsCardList
                                MediaDiscoveryPeriod.Months -> uiState.monthsCardList
                                MediaDiscoveryPeriod.Days -> uiState.daysCardList
                                else -> uiState.daysCardList
                            }
                            MediaDiscoveryCardListView(
                                dateCards = dateCards,
                                onCardClick = onCardClick,
                                fromFolderLink = uiState.fromFolderLink,
                                shouldApplySensitiveMode = uiState.isHiddenNodesEnabled &&
                                        uiState.accountType?.isPaid == true &&
                                        !uiState.isBusinessAccountExpired,
                            )
                        }
                    }
                }
            }

            MediaDiscoveryPeriodChip(
                selectedMediaDiscoveryPeriod = uiState.selectedPeriod,
                onTimeBarTabSelected = onPeriodSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = if (uiState.isUploadAllowed) 76.dp else 0.dp)
                    .align(Alignment.BottomCenter)
                    .applyScrollToHideFabBehavior(),
                isVisible = isPeriodVisible
                        && uiState.mediaListItemList.isNotEmpty()
                        && !uiState.isInSelectionMode
            )
        }

        if (showUploadOptionsBottomSheet) {
            UploadOptionsBottomSheet(
                onUploadFilesClicked = onUploadFilesClicked,
                onUploadFolderClicked = onUploadFolderClicked,
                onScanDocumentClicked = onScanDocumentClicked,
                onCaptureClicked = onCaptureClicked,
                onNewFolderClicked = onNewFolderClicked,
                onNewTextFileClicked = onNewTextFileClicked,
                onDismissSheet = onDismissUploadSheet,
            )
        }
    }

    if (showSortByDialog) {
        MediaDiscoverySortDialog(
            selectedOrder = uiState.currentSort.ordinal,
            onDismissRequest = { showSortByDialog = false },
            onOptionSelected = { sort ->
                onSetCurrentSort(Sort.entries[sort])
                showSortByDialog = false
            },
        )
    }

    if (showFilterDialog) {
        MediaDiscoveryFilterDialog(
            selectedOrder = uiState.currentMediaType.ordinal,
            onDismissRequest = { showFilterDialog = false },
            onOptionSelected = { ordinal ->
                onSetCurrentMediaType(FilterMediaType.entries[ordinal])
                showFilterDialog = false
            },
        )
    }
}

internal const val MEDIA_DISCOVERY_FAB_TAG = "media_discovery_screen:add_content_fab"

@Composable
private fun MediaDiscoveryGridView(
    uiState: CloudDriveMediaDiscoveryUiState,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    onScrollingListener: (Boolean) -> Unit,
    onItemClicked: (Photo) -> Unit,
    onItemLongPressed: (Photo) -> Unit,
    zoomIn: () -> Unit,
    zoomOut: () -> Unit,
) {
    val fileTypeIconMapper = remember { FileTypeIconMapper() }
    val gridCells = remember(uiState.currentZoomLevel) {
        when (uiState.currentZoomLevel) {
            ZoomLevel.Grid_1 -> GridCells.Fixed(1)
            ZoomLevel.Grid_3 -> GridCells.Fixed(3)
            ZoomLevel.Grid_5 -> GridCells.Fixed(5)
        }
    }
    val lazyGridState = rememberSaveable(
        uiState.scrollStartIndex,
        uiState.scrollStartOffset,
        saver = LazyGridState.Saver,
    ) {
        LazyGridState(
            uiState.scrollStartIndex,
            uiState.scrollStartOffset,
        )
    }

    LaunchedEffect(uiState.scrollStartIndex) {
        if (uiState.scrollStartIndex > 0) {
            val startIndex = if (uiState.selectedPeriod == MediaDiscoveryPeriod.All) {
                uiState.scrollStartIndex
            } else {
                // Since CardListView has two headers, the index needs to be incremented by 2.
                uiState.scrollStartIndex + 2
            }
            lazyGridState.animateScrollToItem(startIndex)
        }
    }

    LaunchedEffect(lazyGridState.isScrollInProgress) {
        onScrollingListener(lazyGridState.isScrollInProgress)
    }

    FastScrollLazyVerticalGrid(
        modifier = Modifier
            .photosZoomGestureDetector(
                onZoomIn = zoomIn,
                onZoomOut = zoomOut,
            ),
        state = lazyGridState,
        totalItems = uiState.mediaListItemList.size,
        columns = gridCells,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        contentPadding = contentPadding
    ) {
        item(
            key = "sort_header_item",
            span = { GridItemSpan(maxLineSpan) }
        ) {
            NodeHeaderItem(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                onSortOrderClick = {},
                onChangeViewTypeClick = onBack,
                onEnterMediaDiscoveryClick = {},
                sortConfiguration = NodeSortConfiguration.default,
                isListView = false,
                showSortOrder = false,
                showChangeViewType = true,
                showMediaDiscoveryButton = false,
            )
        }

        items(
            key = { uiState.mediaListItemList[it].key },
            count = uiState.mediaListItemList.size,
            span = { index ->
                val item = uiState.mediaListItemList[index]
                if (item is MediaListItem.Separator) {
                    GridItemSpan(maxLineSpan)
                } else {
                    GridItemSpan(1)
                }
            }
        ) {
            when (val item = uiState.mediaListItemList[it]) {
                is MediaListItem.PhotoItem -> {
                    val photo = item.photo
                    val isSensitive = uiState.isHiddenNodesEnabled &&
                            uiState.showHiddenNodes &&
                            (photo.isSensitive || photo.isSensitiveInherited)

                    MediaGridViewItem(
                        thumbnailData = ThumbnailRequest(
                            id = NodeId(photo.id),
                            isPublicNode = uiState.fromFolderLink,
                        ),
                        defaultImage = fileTypeIconMapper(
                            photo.name.substringAfterLast('.', ""),
                        ),
                        isSensitive = isSensitive,
                        showBlurEffect = isSensitive,
                        showFavourite = photo.isFavourite,
                        isSelected = photo.id in uiState.selectedPhotoIds,
                        onClick = { onItemClicked(photo) },
                        onLongClick = { onItemLongPressed(photo) },
                    )
                }

                is MediaListItem.VideoItem -> {
                    val video = item.video
                    val isSensitive = uiState.isHiddenNodesEnabled &&
                            uiState.showHiddenNodes &&
                            (video.isSensitive || video.isSensitiveInherited)
                    MediaGridViewItem(
                        thumbnailData = ThumbnailRequest(
                            id = NodeId(video.id),
                            isPublicNode = uiState.fromFolderLink,
                        ),
                        defaultImage = fileTypeIconMapper(
                            video.name.substringAfterLast('.', ""),
                        ),
                        duration = item.duration,
                        isSensitive = isSensitive,
                        showBlurEffect = isSensitive,
                        showFavourite = video.isFavourite,
                        isSelected = video.id in uiState.selectedPhotoIds,
                        onClick = { onItemClicked(video) },
                        onLongClick = { onItemLongPressed(video) },
                    )
                }

                is MediaListItem.Separator -> {
                    MediaDiscoverySeparator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .conditional(it == 0) {
                                padding(bottom = 12.dp)
                            }
                            .conditional(it > 0) {
                                padding(vertical = 12.dp)
                            },
                        currentZoomLevel = uiState.currentZoomLevel,
                        modificationTime = item.modificationTime,
                    )
                }
            }
        }

        // Bottom spacer so content is not hidden behind TimeSwitchBar
        item(
            key = "bottom_spacer",
            span = { GridItemSpan(maxLineSpan) }
        ) {
            Spacer(modifier = Modifier.height(56.dp))
        }
    }
}

@SuppressLint("LocalContextConfigurationRead")
@Composable
private fun MediaDiscoverySeparator(
    currentZoomLevel: ZoomLevel,
    modificationTime: LocalDateTime,
    modifier: Modifier = Modifier,
) {
    val locale = LocalContext.current.resources.configuration.locales[0]
    val dateText = remember(modificationTime, currentZoomLevel, locale) {
        formatSeparatorDate(
            currentZoomLevel = currentZoomLevel,
            modificationTime = modificationTime,
            locale = locale,
        )
    }

    MegaText(
        text = dateText,
        textAlign = TextAlign.Start,
        modifier = modifier
    )
}

private fun formatSeparatorDate(
    currentZoomLevel: ZoomLevel,
    modificationTime: LocalDateTime,
    locale: Locale,
): String {
    val datePattern = if (currentZoomLevel == ZoomLevel.Grid_1) {
        if (modificationTime.year == LocalDateTime.now().year) {
            DateFormat.getBestDateTimePattern(
                locale, "dd MMMM"
            )
        } else {
            DateFormat.getBestDateTimePattern(
                locale, "dd MMMM yyyy"
            )
        }
    } else {
        if (modificationTime.year == LocalDateTime.now().year) {
            DateFormat.getBestDateTimePattern(locale, "LLLL")
        } else {
            DateFormat.getBestDateTimePattern(locale, "LLLL yyyy")
        }
    }
    return SimpleDateFormat(datePattern, locale).format(
        Date.from(
            modificationTime
                .toLocalDate()
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant()
        )
    )
}

private enum class MediaDiscoveryDropDownAction(@StringRes val textRes: Int) {
    Filter(sharedR.string.general_action_filter),
    SortBy(sharedR.string.action_sort_by_header),
    ZoomIn(sharedR.string.media_discovery_screen_dropdown_action_zoom_in),
    ZoomOut(sharedR.string.media_discovery_screen_dropdown_action_zoom_out),
}

@CombinedThemePreviews
@Composable
private fun CloudDriveMediaDiscoveryLoadingPreview() {
    AndroidThemeForPreviews {
        CloudDriveMediaDiscoveryScreen(
            uiState = CloudDriveMediaDiscoveryUiState(loadPhotosDone = false),
            actionUiState = NodeActionState(),
            multiNodeActionHandler = MultiNodeActionHandler { _, _ -> },
            showUploadOptionsBottomSheet = false,
            onBack = {},
            onItemClicked = {},
            onItemLongPressed = {},
            selectAllItems = {},
            deselectAllItems = {},
            onMoreOptionsClicked = {},
            onPeriodSelected = {},
            onCardClick = {},
            onFabClicked = {},
            onUploadFilesClicked = {},
            onUploadFolderClicked = {},
            onScanDocumentClicked = {},
            onCaptureClicked = {},
            onNewFolderClicked = {},
            onNewTextFileClicked = {},
            onDismissUploadSheet = {},
            onTransfersClicked = {},
            onClickZoomIn = {},
            onClickZoomOut = {},
            onSetCurrentSort = {},
            onSetCurrentMediaType = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CloudDriveMediaDiscoveryEmptyPreview() {
    AndroidThemeForPreviews {
        CloudDriveMediaDiscoveryScreen(
            uiState = CloudDriveMediaDiscoveryUiState(loadPhotosDone = true),
            actionUiState = NodeActionState(),
            multiNodeActionHandler = MultiNodeActionHandler { _, _ -> },
            showUploadOptionsBottomSheet = false,
            onBack = {},
            onItemClicked = {},
            onItemLongPressed = {},
            selectAllItems = {},
            deselectAllItems = {},
            onMoreOptionsClicked = {},
            onPeriodSelected = {},
            onCardClick = {},
            onFabClicked = {},
            onUploadFilesClicked = {},
            onUploadFolderClicked = {},
            onScanDocumentClicked = {},
            onCaptureClicked = {},
            onNewFolderClicked = {},
            onNewTextFileClicked = {},
            onDismissUploadSheet = {},
            onTransfersClicked = {},
            onClickZoomIn = {},
            onClickZoomOut = {},
            onSetCurrentSort = {},
            onSetCurrentMediaType = {},
        )
    }
}
