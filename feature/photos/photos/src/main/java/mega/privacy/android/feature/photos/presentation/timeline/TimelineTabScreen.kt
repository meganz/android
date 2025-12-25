package mega.privacy.android.feature.photos.presentation.timeline

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.chip.SelectionChipStyle
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.modifiers.conditional
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.model.CameraUploadsStatus
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.MediaCameraUploadUiState
import mega.privacy.android.feature.photos.presentation.component.PhotosNodeGridView
import mega.privacy.android.feature.photos.presentation.timeline.component.CameraUploadsBanner
import mega.privacy.android.feature.photos.presentation.timeline.component.EnableCameraUploadsContent
import mega.privacy.android.feature.photos.presentation.timeline.component.PhotosNodeListCardListView
import mega.privacy.android.feature.photos.presentation.timeline.component.PhotosSkeletonView
import mega.privacy.android.feature.photos.presentation.timeline.component.TimelineSortDialog
import mega.privacy.android.feature.photos.presentation.timeline.model.CameraUploadsBannerType
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotoModificationTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import mega.privacy.android.navigation.destination.LegacySettingsCameraUploadsActivityNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun TimelineTabRoute(
    uiState: TimelineTabUiState,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    timelineFilterUiState: TimelineFilterUiState,
    showTimelineSortDialog: Boolean,
    selectedTimePeriod: PhotoModificationTimePeriod,
    clearCameraUploadsMessage: () -> Unit,
    clearCameraUploadsCompletedMessage: () -> Unit,
    loadNextPage: () -> Unit,
    onNavigateToCameraUploadsSettings: (key: LegacySettingsCameraUploadsActivityNavKey) -> Unit,
    setEnableCUPage: (Boolean) -> Unit,
    onGridSizeChange: (value: TimelineGridSize) -> Unit,
    onSortDialogDismissed: () -> Unit,
    onSortOptionChange: (value: TimelineTabSortOptions) -> Unit,
    onPhotoClick: (node: PhotoNodeUiState) -> Unit,
    onPhotoSelected: (node: PhotoNodeUiState) -> Unit,
    onDismissEnableCameraUploadsBanner: () -> Unit,
    handleCameraUploadsPermissionsResult: () -> Unit,
    updateIsWarningBannerShown: (value: Boolean) -> Unit,
    onTabsVisibilityChange: (shouldHide: Boolean) -> Unit,
    onNavigateToUpgradeAccount: (key: UpgradeAccountNavKey) -> Unit,
    onPhotoTimePeriodSelected: (PhotoModificationTimePeriod) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    TimelineTabScreen(
        modifier = modifier,
        contentPadding = contentPadding,
        uiState = uiState,
        timelineFilterUiState = timelineFilterUiState,
        mediaCameraUploadUiState = mediaCameraUploadUiState,
        showTimelineSortDialog = showTimelineSortDialog,
        selectedTimePeriod = selectedTimePeriod,
        clearCameraUploadsMessage = clearCameraUploadsMessage,
        clearCameraUploadsCompletedMessage = clearCameraUploadsCompletedMessage,
        loadNextPage = loadNextPage,
        onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings,
        setEnableCUPage = setEnableCUPage,
        onGridSizeChange = onGridSizeChange,
        onSortDialogDismissed = onSortDialogDismissed,
        onSortOptionChange = onSortOptionChange,
        onPhotoClick = onPhotoClick,
        onPhotoSelected = onPhotoSelected,
        onDismissEnableCameraUploadsBanner = onDismissEnableCameraUploadsBanner,
        handleCameraUploadsPermissionsResult = handleCameraUploadsPermissionsResult,
        updateIsWarningBannerShown = updateIsWarningBannerShown,
        onTabsVisibilityChange = onTabsVisibilityChange,
        onNavigateToUpgradeAccount = onNavigateToUpgradeAccount,
        onPhotoTimePeriodSelected = onPhotoTimePeriodSelected
    )
}

@Composable
internal fun TimelineTabScreen(
    uiState: TimelineTabUiState,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    timelineFilterUiState: TimelineFilterUiState,
    showTimelineSortDialog: Boolean,
    selectedTimePeriod: PhotoModificationTimePeriod,
    clearCameraUploadsMessage: () -> Unit,
    clearCameraUploadsCompletedMessage: () -> Unit,
    loadNextPage: () -> Unit,
    onNavigateToCameraUploadsSettings: (key: LegacySettingsCameraUploadsActivityNavKey) -> Unit,
    setEnableCUPage: (Boolean) -> Unit,
    onGridSizeChange: (value: TimelineGridSize) -> Unit,
    onSortDialogDismissed: () -> Unit,
    onSortOptionChange: (value: TimelineTabSortOptions) -> Unit,
    onPhotoClick: (node: PhotoNodeUiState) -> Unit,
    onPhotoSelected: (node: PhotoNodeUiState) -> Unit,
    onDismissEnableCameraUploadsBanner: () -> Unit,
    handleCameraUploadsPermissionsResult: () -> Unit,
    updateIsWarningBannerShown: (value: Boolean) -> Unit,
    onTabsVisibilityChange: (shouldHide: Boolean) -> Unit,
    onNavigateToUpgradeAccount: (key: UpgradeAccountNavKey) -> Unit,
    onPhotoTimePeriodSelected: (PhotoModificationTimePeriod) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val resource = LocalResources.current
    val snackBarHostState = LocalSnackBarHostState.current

    val lazyGridState = rememberLazyGridState()
    val isGridScrollingDown by lazyGridState.isScrollingDown()
    val isGridScrolledToEnd by lazyGridState.isScrolledToEnd()
    val isGridScrolledToTop by lazyGridState.isScrolledToTop()
    val lazyListState = rememberLazyListState()
    val isListScrollingDown by lazyListState.isScrollingDown()
    val isListScrolledToEnd by lazyListState.isScrolledToEnd()
    val isListScrolledToTop by lazyListState.isScrolledToTop()
    val shouldShowTimePeriodSelector by remember(selectedTimePeriod) {
        derivedStateOf {
            if (selectedTimePeriod == PhotoModificationTimePeriod.All) {
                !lazyGridState.isScrollInProgress || (!isGridScrollingDown && !isGridScrolledToEnd) || isGridScrolledToTop
            } else !lazyListState.isScrollInProgress || (!isListScrollingDown && !isListScrolledToEnd) || isListScrolledToTop
        }
    }
    var shouldScrollToIndex by remember { mutableIntStateOf(0) }
    val showEnableCUPage = mediaCameraUploadUiState.enableCameraUploadPageShowing
            && timelineFilterUiState.mediaSource != FilterMediaSource.CloudDrive
    var cuBannerType by remember {
        mutableStateOf(
            value = getCameraUploadsBannerType(
                timelineTabUiState = uiState,
                mediaCameraUploadUiState = mediaCameraUploadUiState
            )
        )
    }
    val cameraUploadsPermissionsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            handleCameraUploadsPermissionsResult()
        }
    var isWarningBannerStateValid by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(
        mediaCameraUploadUiState.enableCameraUploadButtonShowing,
        uiState.selectedPhotoCount,
        mediaCameraUploadUiState.showCameraUploadsWarning,
        mediaCameraUploadUiState.cameraUploadsStatus,
        mediaCameraUploadUiState.cameraUploadsFinishedReason,
        mediaCameraUploadUiState.isCUPausedWarningBannerEnabled
    ) {
        cuBannerType = getCameraUploadsBannerType(
            timelineTabUiState = uiState,
            mediaCameraUploadUiState = mediaCameraUploadUiState
        )
    }

    LaunchedEffect(shouldScrollToIndex) {
        if (selectedTimePeriod == PhotoModificationTimePeriod.All) {
            lazyGridState.scrollToItem(shouldScrollToIndex)
        } else lazyListState.animateScrollToItem(shouldScrollToIndex)
    }

    LaunchedEffect(mediaCameraUploadUiState.cameraUploadsMessage) {
        if (mediaCameraUploadUiState.cameraUploadsMessage.isNotEmpty()) {
            snackBarHostState?.showAutoDurationSnackbar(
                message = mediaCameraUploadUiState.cameraUploadsMessage,
            )
            clearCameraUploadsMessage()
        }
    }

    LaunchedEffect(mediaCameraUploadUiState.showCameraUploadsCompletedMessage) {
        if (mediaCameraUploadUiState.showCameraUploadsCompletedMessage) {
            snackBarHostState?.showAutoDurationSnackbar(
                message = resource.getQuantityString(
                    sharedR.plurals.timeline_tab_camera_uploads_completed,
                    mediaCameraUploadUiState.cameraUploadsTotalUploaded,
                    mediaCameraUploadUiState.cameraUploadsTotalUploaded,
                )
            )
        }
        clearCameraUploadsCompletedMessage()
    }

    LaunchedEffect(lazyGridState) {
        snapshotFlow { lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleItemIndex ->
                if (lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == lazyGridState.layoutInfo.totalItemsCount - 1) {
                    loadNextPage()
                }
            }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleItemIndex ->
                if (lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == lazyListState.layoutInfo.totalItemsCount - 1) {
                    loadNextPage()
                }
            }
    }

    LaunchedEffect(
        mediaCameraUploadUiState.showCameraUploadsWarning,
        mediaCameraUploadUiState.isCameraUploadsLimitedAccess,
        mediaCameraUploadUiState.cameraUploadsFinishedReason,
        mediaCameraUploadUiState.isWarningBannerShown,
        mediaCameraUploadUiState.isCUPausedWarningBannerEnabled
    ) {
        isWarningBannerStateValid = mediaCameraUploadUiState.isWarningBannerShown &&
                isCUBannerTypeValidForWarningBanner(
                    bannerType = cuBannerType,
                    mediaCameraUploadUiState = mediaCameraUploadUiState
                )
    }

    LaunchedEffect(
        isGridScrollingDown,
        isGridScrolledToEnd,
        isGridScrolledToTop,
        isListScrollingDown,
        isListScrolledToEnd,
        isListScrolledToTop
    ) {
        val shouldShowTabs = if (selectedTimePeriod == PhotoModificationTimePeriod.All) {
            isGridScrolledToTop || (!isGridScrollingDown && !isGridScrolledToEnd)
        } else isListScrolledToTop || (!isListScrollingDown && !isListScrolledToEnd)
        onTabsVisibilityChange(!shouldShowTabs)
    }

    if (showTimelineSortDialog) {
        TimelineSortDialog(
            modifier = Modifier.testTag(TIMELINE_TAB_SCREEN_SORT_DIALOG_TAG),
            selected = uiState.currentSort,
            onDismissRequest = onSortDialogDismissed,
            onOptionSelected = onSortOptionChange
        )
    }

    when {
        showEnableCUPage -> {
            EnableCameraUploadsContent(
                modifier = modifier
                    .padding(horizontal = 16.dp)
                    .testTag(TIMELINE_TAB_SCREEN_ENABLE_CU_CONTENT_TAG),
                onEnable = {
                    onNavigateToCameraUploadsSettings(
                        LegacySettingsCameraUploadsActivityNavKey()
                    )
                }
            )
        }

        uiState.isLoading -> {
            PhotosSkeletonView(
                modifier = Modifier.testTag(TIMELINE_TAB_SCREEN_LOADING_SKELETON_VIEW_TAG)
            )
        }

        uiState.displayedPhotos.isEmpty() -> {
            EmptyBody(
                modifier = Modifier.testTag(TIMELINE_TAB_SCREEN_EMPTY_BODY_TAG),
                enableCameraUploadButtonShowing = mediaCameraUploadUiState.enableCameraUploadButtonShowing,
                mediaSource = timelineFilterUiState.mediaSource,
                setEnableCUPage = setEnableCUPage,
            )
        }

        else -> {
            TimelineTabContent(
                modifier = modifier,
                contentPadding = contentPadding,
                uiState = uiState,
                shouldShowEnableCUBanner = mediaCameraUploadUiState.shouldShowEnableCUBanner,
                shouldShowCUWarningBanner = isWarningBannerStateValid,
                lazyGridState = lazyGridState,
                lazyListState = lazyListState,
                selectedTimePeriod = selectedTimePeriod,
                shouldShowTimePeriodSelector = shouldShowTimePeriodSelector,
                bannerType = cuBannerType,
                onPhotoTimePeriodSelected = onPhotoTimePeriodSelected,
                onGridSizeChange = onGridSizeChange,
                onPhotoClick = onPhotoClick,
                onPhotoSelected = onPhotoSelected,
                onPhotosNodeListCardClick = { photo ->
                    onPhotoTimePeriodSelected(PhotoModificationTimePeriod.entries[selectedTimePeriod.ordinal + 1])
                    shouldScrollToIndex = calculateScrollIndex(
                        photo = photo,
                        displayedPhotos = uiState.displayedPhotos,
                        daysCardPhotos = uiState.daysCardPhotos,
                        monthsCardPhotos = uiState.monthsCardPhotos,
                    )
                },
                onDismissEnableCameraUploadsBanner = onDismissEnableCameraUploadsBanner,
                onChangeCameraUploadsPermissions = {
                    val cameraUploadsPermissions = getCameraUploadsPermissions()
                    cameraUploadsPermissionsLauncher.launch(cameraUploadsPermissions)
                },
                onDismissWarningBanner = { updateIsWarningBannerShown(false) },
                onNavigateToCameraUploadsSettings = {
                    onNavigateToCameraUploadsSettings(
                        LegacySettingsCameraUploadsActivityNavKey()
                    )
                },
                onNavigateMobileDataSetting = {
                    onNavigateToCameraUploadsSettings(
                        LegacySettingsCameraUploadsActivityNavKey(isShowHowToUploadPrompt = true)
                    )
                },
                onNavigateToUpgradeScreen = {
                    onNavigateToUpgradeAccount(UpgradeAccountNavKey())
                },
            )
        }
    }
}

@Composable
private fun EmptyBody(
    mediaSource: FilterMediaSource,
    enableCameraUploadButtonShowing: Boolean,
    setEnableCUPage: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (enableCameraUploadButtonShowing && mediaSource != FilterMediaSource.CloudDrive) {
        setEnableCUPage(true)
    } else {
        EmptyBodyContent(
            modifier = modifier
                .fillMaxSize()
                .conditional(LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE) {
                    verticalScroll(rememberScrollState())
                },
        )
    }
}

@Composable
private fun EmptyBodyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            modifier = Modifier.size(120.dp),
            painter = painterResource(R.drawable.il_glass_image),
            contentDescription = "No media found"
        )

        MegaText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            text = stringResource(sharedR.string.timeline_tab_empty_body_no_media_found),
            textAlign = TextAlign.Center,
            style = AppTheme.typography.bodyLarge,
            textColor = TextColor.Secondary
        )
    }
}

@Composable
private fun TimelineTabContent(
    uiState: TimelineTabUiState,
    shouldShowEnableCUBanner: Boolean,
    shouldShowCUWarningBanner: Boolean,
    lazyGridState: LazyGridState,
    lazyListState: LazyListState,
    selectedTimePeriod: PhotoModificationTimePeriod,
    shouldShowTimePeriodSelector: Boolean,
    bannerType: CameraUploadsBannerType,
    onPhotoTimePeriodSelected: (PhotoModificationTimePeriod) -> Unit,
    onGridSizeChange: (value: TimelineGridSize) -> Unit,
    onPhotoClick: (node: PhotoNodeUiState) -> Unit,
    onPhotoSelected: (node: PhotoNodeUiState) -> Unit,
    onPhotosNodeListCardClick: (photo: PhotosNodeListCard) -> Unit,
    onDismissEnableCameraUploadsBanner: () -> Unit,
    onChangeCameraUploadsPermissions: () -> Unit,
    onDismissWarningBanner: () -> Unit,
    onNavigateToCameraUploadsSettings: () -> Unit,
    onNavigateMobileDataSetting: () -> Unit,
    onNavigateToUpgradeScreen: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val shouldShowCUBanner by remember(
        key1 = uiState.selectedPhotoCount,
        key2 = bannerType
    ) {
        derivedStateOf {
            uiState.selectedPhotoCount == 0 && bannerType != CameraUploadsBannerType.NONE
        }
    }
    Box(modifier = modifier) {
        when (selectedTimePeriod) {
            PhotoModificationTimePeriod.All -> {
                PhotosNodeGridView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TIMELINE_TAB_CONTENT_GRID_VIEW_TAG),
                    lazyGridState = lazyGridState,
                    contentPadding = contentPadding,
                    items = uiState.displayedPhotos,
                    gridSize = uiState.gridSize,
                    onGridSizeChange = onGridSizeChange,
                    onClick = onPhotoClick,
                    onLongClick = onPhotoSelected,
                    header = {
                        if (shouldShowCUBanner) {
                            CameraUploadsBanner(
                                bannerType = bannerType,
                                shouldShowEnableCUBanner = shouldShowEnableCUBanner,
                                shouldShowCUWarningBanner = shouldShowCUWarningBanner,
                                onEnableCameraUploads = onNavigateToCameraUploadsSettings,
                                onDismissEnableCameraUploadsBanner = onDismissEnableCameraUploadsBanner,
                                onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions,
                                onDismissWarningBanner = onDismissWarningBanner,
                                onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings,
                                onNavigateMobileDataSetting = onNavigateMobileDataSetting,
                                onNavigateUpgradeScreen = onNavigateToUpgradeScreen,
                            )
                        }
                    }
                )
            }

            else -> {
                val items = when (selectedTimePeriod) {
                    PhotoModificationTimePeriod.Years -> uiState.yearsCardPhotos
                    PhotoModificationTimePeriod.Months -> uiState.monthsCardPhotos
                    else -> uiState.daysCardPhotos
                }
                PhotosNodeListCardListView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TIMELINE_TAB_CONTENT_LIST_VIEW_TAG),
                    state = lazyListState,
                    contentPadding = PaddingValues(bottom = 50.dp),
                    photos = items,
                    onClick = onPhotosNodeListCardClick,
                    header = {
                        if (shouldShowCUBanner) {
                            CameraUploadsBanner(
                                bannerType = bannerType,
                                shouldShowEnableCUBanner = shouldShowEnableCUBanner,
                                shouldShowCUWarningBanner = shouldShowCUWarningBanner,
                                onEnableCameraUploads = onNavigateToCameraUploadsSettings,
                                onDismissEnableCameraUploadsBanner = onDismissEnableCameraUploadsBanner,
                                onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions,
                                onDismissWarningBanner = onDismissWarningBanner,
                                onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings,
                                onNavigateMobileDataSetting = onNavigateMobileDataSetting,
                                onNavigateUpgradeScreen = onNavigateToUpgradeScreen,
                            )
                        }
                    }
                )
            }
        }

        if (uiState.selectedPhotoCount == 0) {
            PhotoModificationTimePeriodSelector(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .testTag(TIMELINE_TAB_CONTENT_PHOTO_MODIFICATION_TIME_PERIOD_SELECTOR_TAG),
                isVisible = shouldShowTimePeriodSelector,
                selectedTimePeriod = selectedTimePeriod,
                onPhotoTimePeriodSelected = onPhotoTimePeriodSelected
            )
        }
    }
}

@Composable
private fun PhotoModificationTimePeriodSelector(
    isVisible: Boolean,
    selectedTimePeriod: PhotoModificationTimePeriod,
    onPhotoTimePeriodSelected: (PhotoModificationTimePeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible,
        exit = slideOutVertically { it },
        enter = slideInVertically { it },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .requiredWidthIn(max = 360.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            PhotoModificationTimePeriod.entries.forEachIndexed { index, timePeriod ->
                MegaChip(
                    onClick = { onPhotoTimePeriodSelected(timePeriod) },
                    selected = selectedTimePeriod == timePeriod,
                    text = stringResource(id = timePeriod.stringResId),
                    style = SelectionChipStyle,
                )

                if (index != PhotoModificationTimePeriod.entries.lastIndex) {
                    Spacer(
                        modifier = Modifier
                            .width(8.dp)
                            .testTag(PHOTO_MODIFICATION_TIME_PERIOD_SELECTOR_SPACER_TAG)
                    )
                }
            }
        }
    }
}

@Composable
private fun LazyGridState.isScrollingDown(): State<Boolean> {
    var nextIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var nextScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (nextIndex != firstVisibleItemIndex) {
                nextIndex < firstVisibleItemIndex
            } else {
                nextScrollOffset <= firstVisibleItemScrollOffset
            }.also {
                nextIndex = firstVisibleItemIndex
                nextScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }
}

@Composable
private fun LazyListState.isScrollingDown(): State<Boolean> {
    var nextIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var nextScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (nextIndex != firstVisibleItemIndex) {
                nextIndex < firstVisibleItemIndex
            } else {
                nextScrollOffset <= firstVisibleItemScrollOffset
            }.also {
                nextIndex = firstVisibleItemIndex
                nextScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }
}

@Composable
private fun LazyGridState.isScrolledToEnd() = remember(this) {
    derivedStateOf {
        layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1
    }
}

@Composable
private fun LazyListState.isScrolledToEnd() = remember(this) {
    derivedStateOf {
        layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1
    }
}

@Composable
private fun LazyGridState.isScrolledToTop() = remember(this) {
    derivedStateOf {
        firstVisibleItemIndex <= 1 && firstVisibleItemScrollOffset == 0
    }
}

@Composable
private fun LazyListState.isScrolledToTop() = remember(this) {
    derivedStateOf {
        firstVisibleItemIndex <= 1 && firstVisibleItemScrollOffset == 0
    }
}

private fun calculateScrollIndex(
    photo: PhotosNodeListCard,
    displayedPhotos: List<PhotosNodeContentType>,
    daysCardPhotos: List<PhotosNodeListCard>,
    monthsCardPhotos: List<PhotosNodeListCard>,
): Int {
    return when (photo) {
        is PhotosNodeListCard.Years -> {
            val photo = monthsCardPhotos.find {
                it as PhotosNodeListCard.Months
                it.photoItem.photo.modificationTime == photo.photoItem.photo.modificationTime
            }
            monthsCardPhotos.indexOf(photo)
        }

        is PhotosNodeListCard.Months -> {
            val photo = daysCardPhotos.find {
                it as PhotosNodeListCard.Days
                it.photoItem.photo.modificationTime == photo.photoItem.photo.modificationTime
            }
            daysCardPhotos.indexOf(photo)
        }

        is PhotosNodeListCard.Days -> {
            val photo = displayedPhotos.find { it.key == photo.photoItem.photo.hashCode() }
            displayedPhotos.indexOf(photo)
        }
    }
}

private fun getCameraUploadsBannerType(
    timelineTabUiState: TimelineTabUiState,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
): CameraUploadsBannerType {
    return when {
        mediaCameraUploadUiState.enableCameraUploadButtonShowing && timelineTabUiState.selectedPhotoCount == 0 ->
            CameraUploadsBannerType.EnableCameraUploads

        mediaCameraUploadUiState.isCUPausedWarningBannerEnabled &&
                mediaCameraUploadUiState.cameraUploadsFinishedReason == CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA
            -> CameraUploadsBannerType.FullStorage

        mediaCameraUploadUiState.isCUPausedWarningBannerEnabled &&
                mediaCameraUploadUiState.cameraUploadsFinishedReason == CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET
            -> CameraUploadsBannerType.NetworkRequirementNotMet

        mediaCameraUploadUiState.isCUPausedWarningBannerEnabled &&
                mediaCameraUploadUiState.cameraUploadsFinishedReason == CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW
            -> CameraUploadsBannerType.LowBattery

        mediaCameraUploadUiState.isCUPausedWarningBannerEnabled &&
                mediaCameraUploadUiState.cameraUploadsFinishedReason == CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET
            -> CameraUploadsBannerType.DeviceChargingNotMet

        mediaCameraUploadUiState.isCameraUploadsLimitedAccess -> CameraUploadsBannerType.NoFullAccess

        mediaCameraUploadUiState.cameraUploadsStatus == CameraUploadsStatus.Sync ->
            CameraUploadsBannerType.CheckingUploads

        mediaCameraUploadUiState.cameraUploadsStatus == CameraUploadsStatus.Uploading ->
            CameraUploadsBannerType.PendingCount

        else -> CameraUploadsBannerType.NONE
    }
}

private fun isCUBannerTypeValidForWarningBanner(
    bannerType: CameraUploadsBannerType,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
) = when (bannerType) {
    CameraUploadsBannerType.FullStorage ->
        mediaCameraUploadUiState.cameraUploadsFinishedReason == CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA

    CameraUploadsBannerType.NetworkRequirementNotMet ->
        mediaCameraUploadUiState.cameraUploadsFinishedReason == CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET

    CameraUploadsBannerType.NoFullAccess -> mediaCameraUploadUiState.isCameraUploadsLimitedAccess

    CameraUploadsBannerType.DeviceChargingNotMet ->
        mediaCameraUploadUiState.cameraUploadsFinishedReason == CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET

    CameraUploadsBannerType.LowBattery ->
        mediaCameraUploadUiState.cameraUploadsFinishedReason == CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW

    else -> false
}

private fun getCameraUploadsPermissions() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            POST_NOTIFICATIONS,
            getImagePermissionByVersion(),
            getVideoPermissionByVersion(),
            READ_MEDIA_VISUAL_USER_SELECTED,
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            POST_NOTIFICATIONS,
            getImagePermissionByVersion(),
            getVideoPermissionByVersion()
        )
    } else {
        arrayOf(
            getImagePermissionByVersion(),
            getVideoPermissionByVersion()
        )
    }

private fun getImagePermissionByVersion() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        READ_MEDIA_IMAGES
    } else {
        READ_EXTERNAL_STORAGE
    }

private fun getVideoPermissionByVersion() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        READ_MEDIA_VIDEO
    } else {
        READ_EXTERNAL_STORAGE
    }

@CombinedThemePreviews
@Composable
private fun TimelineTabScreenPreview() {
    AndroidThemeForPreviews {
        TimelineTabScreen(
            modifier = Modifier.fillMaxSize(),
            uiState = TimelineTabUiState(isLoading = false),
            mediaCameraUploadUiState = MediaCameraUploadUiState(),
            timelineFilterUiState = TimelineFilterUiState(),
            showTimelineSortDialog = false,
            selectedTimePeriod = PhotoModificationTimePeriod.All,
            clearCameraUploadsMessage = {},
            clearCameraUploadsCompletedMessage = {},
            loadNextPage = {},
            setEnableCUPage = {},
            onNavigateToCameraUploadsSettings = {},
            onGridSizeChange = {},
            onSortDialogDismissed = {},
            onSortOptionChange = {},
            onPhotoClick = {},
            onPhotoSelected = {},
            onDismissEnableCameraUploadsBanner = {},
            handleCameraUploadsPermissionsResult = {},
            updateIsWarningBannerShown = {},
            onTabsVisibilityChange = {},
            onNavigateToUpgradeAccount = {},
            onPhotoTimePeriodSelected = {}
        )
    }
}

internal const val TIMELINE_TAB_SCREEN_SORT_DIALOG_TAG = "timeline_tab_screen:dialog_sort"
internal const val TIMELINE_TAB_SCREEN_ENABLE_CU_CONTENT_TAG =
    "timeline_tab_screen:enable_cu_content"
internal const val TIMELINE_TAB_SCREEN_LOADING_SKELETON_VIEW_TAG =
    "timeline_tab_screen:view_loading_skeleton"
internal const val TIMELINE_TAB_SCREEN_EMPTY_BODY_TAG =
    "timeline_tab_screen:empty_body"
internal const val TIMELINE_TAB_CONTENT_GRID_VIEW_TAG =
    "timeline_tab_content:grid_view"
internal const val TIMELINE_TAB_CONTENT_LIST_VIEW_TAG =
    "timeline_tab_content:list_view"
internal const val TIMELINE_TAB_CONTENT_PHOTO_MODIFICATION_TIME_PERIOD_SELECTOR_TAG =
    "timeline_tab_content:selector_photo_modification_time_period"
internal const val PHOTO_MODIFICATION_TIME_PERIOD_SELECTOR_SPACER_TAG =
    "photo_modification_time_period_selector:spacer"
