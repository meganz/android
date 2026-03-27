package mega.privacy.android.feature.photos.presentation.timeline

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.modifiers.conditional
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.CUStatusUiState
import mega.privacy.android.feature.photos.presentation.MediaCameraUploadUiState
import mega.privacy.android.feature.photos.presentation.component.MediaTimePeriodSelector
import mega.privacy.android.feature.photos.presentation.component.PhotosNodeGridView
import mega.privacy.android.feature.photos.presentation.timeline.component.CameraUploadsBanner
import mega.privacy.android.feature.photos.presentation.timeline.component.EnableCameraUploadsContent
import mega.privacy.android.feature.photos.presentation.timeline.component.PhotosNodeListCardListView
import mega.privacy.android.feature.photos.presentation.timeline.component.PhotosSkeletonView
import mega.privacy.android.feature.photos.presentation.timeline.component.TimelineSortDialog
import mega.privacy.android.feature.photos.presentation.timeline.model.MediaTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import mega.privacy.android.feature.photos.presentation.timeline.state.rememberTimelineLazyListState
import mega.privacy.android.navigation.destination.LegacySettingsCameraUploadsActivityNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.MediaScreenAllFilterSelectedEvent
import mega.privacy.mobile.analytics.event.MediaScreenDaysFilterSelectedEvent
import mega.privacy.mobile.analytics.event.MediaScreenMonthsFilterSelectedEvent
import mega.privacy.mobile.analytics.event.MediaScreenYearsFilterSelectedEvent
import timber.log.Timber

@Composable
internal fun TimelineTabRoute(
    uiState: TimelineTabUiState,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    timelineFilterUiState: TimelineFilterUiState,
    selectedPhotoIds: Set<Long>,
    showTimelineSortDialog: Boolean,
    selectedTimePeriod: MediaTimePeriod,
    clearCameraUploadsCompletedMessage: () -> Unit,
    onNavigateToCameraUploadsSettings: (key: LegacySettingsCameraUploadsActivityNavKey) -> Unit,
    setEnableCUPage: (Boolean) -> Unit,
    onGridSizeChange: (value: TimelineGridSize) -> Unit,
    onSortDialogDismissed: () -> Unit,
    onSortOptionChange: (value: TimelineTabSortOptions) -> Unit,
    onPhotoClick: (node: PhotoNodeUiState) -> Unit,
    onPhotoSelected: (node: PhotoNodeUiState) -> Unit,
    handleCameraUploadsPermissionsResult: () -> Unit,
    handleNotificationPermissionResult: () -> Unit,
    onCUBannerDismissRequest: (status: CUStatusUiState) -> Unit,
    onNavigateToUpgradeAccount: (key: UpgradeAccountNavKey) -> Unit,
    onMediaTimePeriodSelected: (MediaTimePeriod) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    TimelineTabScreen(
        modifier = modifier,
        contentPadding = contentPadding,
        uiState = uiState,
        timelineFilterUiState = timelineFilterUiState,
        mediaCameraUploadUiState = mediaCameraUploadUiState,
        selectedPhotoIds = selectedPhotoIds,
        showTimelineSortDialog = showTimelineSortDialog,
        selectedTimePeriod = selectedTimePeriod,
        clearCameraUploadsCompletedMessage = clearCameraUploadsCompletedMessage,
        onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings,
        setEnableCUPage = setEnableCUPage,
        onGridSizeChange = onGridSizeChange,
        onSortDialogDismissed = onSortDialogDismissed,
        onSortOptionChange = onSortOptionChange,
        onPhotoClick = onPhotoClick,
        onPhotoSelected = onPhotoSelected,
        handleCameraUploadsPermissionsResult = handleCameraUploadsPermissionsResult,
        handleNotificationPermissionResult = handleNotificationPermissionResult,
        onCUBannerDismissRequest = onCUBannerDismissRequest,
        onNavigateToUpgradeAccount = onNavigateToUpgradeAccount,
        onMediaTimePeriodSelected = onMediaTimePeriodSelected
    )
}

@Composable
internal fun TimelineTabScreen(
    uiState: TimelineTabUiState,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    timelineFilterUiState: TimelineFilterUiState,
    selectedPhotoIds: Set<Long>,
    showTimelineSortDialog: Boolean,
    selectedTimePeriod: MediaTimePeriod,
    clearCameraUploadsCompletedMessage: () -> Unit,
    onNavigateToCameraUploadsSettings: (key: LegacySettingsCameraUploadsActivityNavKey) -> Unit,
    setEnableCUPage: (Boolean) -> Unit,
    onGridSizeChange: (value: TimelineGridSize) -> Unit,
    onSortDialogDismissed: () -> Unit,
    onSortOptionChange: (value: TimelineTabSortOptions) -> Unit,
    onPhotoClick: (node: PhotoNodeUiState) -> Unit,
    onPhotoSelected: (node: PhotoNodeUiState) -> Unit,
    handleCameraUploadsPermissionsResult: () -> Unit,
    handleNotificationPermissionResult: () -> Unit,
    onCUBannerDismissRequest: (status: CUStatusUiState) -> Unit,
    onNavigateToUpgradeAccount: (key: UpgradeAccountNavKey) -> Unit,
    onMediaTimePeriodSelected: (MediaTimePeriod) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val activity = LocalActivity.current
    val resource = LocalResources.current
    val snackBarHostState = LocalSnackBarHostState.current
    val scope = rememberCoroutineScope()

    val timelineLazyListState =
        rememberTimelineLazyListState(selectedTimePeriod = selectedTimePeriod)
    val isScrollingDown by timelineLazyListState.isScrollingDown
    val isScrolledToEnd by timelineLazyListState.isScrolledToEnd
    val isScrolledToTop by timelineLazyListState.isScrolledToTop

    val shouldShowTimePeriodSelector by remember(selectedTimePeriod, uiState.isLoading) {
        derivedStateOf {
            if (uiState.isLoading) return@derivedStateOf false
            !timelineLazyListState.isScrollInProgress || (!isScrollingDown && !isScrolledToEnd) || isScrolledToTop
        }
    }
    var shouldScrollToIndex by remember { mutableIntStateOf(-1) }
    val showEnableCUPage = mediaCameraUploadUiState.enableCameraUploadPageShowing
            && timelineFilterUiState.mediaSource != FilterMediaSource.CloudDrive
    val cameraUploadsPermissionsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            handleCameraUploadsPermissionsResult()
        }
    var isNotificationPermissionPermanentlyDenied by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity?.let { currentActivity ->
                    val shouldShowRationale = ActivityCompat
                        .shouldShowRequestPermissionRationale(currentActivity, POST_NOTIFICATIONS)
                    if (!shouldShowRationale) {
                        isNotificationPermissionPermanentlyDenied = true
                    }
                }
            }
            handleNotificationPermissionResult()
        }

    if (uiState.isLoading.not()) {
        LaunchedEffect(shouldScrollToIndex, selectedTimePeriod) {
            if (shouldScrollToIndex > -1) {
                val isCUBannerVisible = if (selectedTimePeriod == MediaTimePeriod.All) {
                    timelineLazyListState.totalItemsCount > uiState.displayedPhotos.size
                } else {
                    val items = when (selectedTimePeriod) {
                        MediaTimePeriod.Years -> uiState.yearsCardPhotos
                        MediaTimePeriod.Months -> uiState.monthsCardPhotos
                        else -> uiState.daysCardPhotos
                    }
                    timelineLazyListState.totalItemsCount > items.size
                }
                val index =
                    if (isCUBannerVisible && shouldScrollToIndex > 0) shouldScrollToIndex + 1 else shouldScrollToIndex
                timelineLazyListState.scrollToItem(index = index)
                shouldScrollToIndex = -1
            }
        }
    }

    EventEffect(
        event = mediaCameraUploadUiState.uploadComplete,
        onConsumed = clearCameraUploadsCompletedMessage
    ) {
        scope.launch {
            snackBarHostState?.showAutoDurationSnackbar(
                message = resource.getQuantityString(
                    sharedR.plurals.timeline_tab_camera_uploads_completed,
                    it,
                    it,
                )
            )
        }
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
                enableCameraUploadButtonShowing = mediaCameraUploadUiState.status is CUStatusUiState.Disabled,
                mediaSource = timelineFilterUiState.mediaSource,
                setEnableCUPage = setEnableCUPage,
            )
        }

        else -> {
            TimelineTabContent(
                modifier = modifier,
                contentPadding = contentPadding,
                uiState = uiState,
                cuStatusUiState = mediaCameraUploadUiState.status,
                selectedPhotoIds = selectedPhotoIds,
                lazyGridState = timelineLazyListState.lazyGridState,
                lazyListState = timelineLazyListState.lazyListState,
                selectedTimePeriod = selectedTimePeriod,
                shouldShowTimePeriodSelector = shouldShowTimePeriodSelector,
                onMediaTimePeriodSelected = {
                    val scrollIndex =
                        timelineLazyListState.calculateScrollIndexBasedOnTimePeriodClick(
                            targetPeriod = it,
                            displayedPhotos = uiState.displayedPhotos,
                            daysCardPhotos = uiState.daysCardPhotos,
                            monthsCardPhotos = uiState.monthsCardPhotos,
                            yearsCardPhotos = uiState.yearsCardPhotos,
                        )
                    if (scrollIndex > -1) {
                        shouldScrollToIndex = scrollIndex
                    }
                    onMediaTimePeriodSelected(it)
                },
                onGridSizeChange = onGridSizeChange,
                onPhotoClick = onPhotoClick,
                onPhotoSelected = onPhotoSelected,
                onPhotosNodeListCardClick = { photo ->
                    onMediaTimePeriodSelected(MediaTimePeriod.entries[selectedTimePeriod.ordinal + 1])
                    shouldScrollToIndex =
                        timelineLazyListState.calculateScrollIndexBasedOnItemClick(
                            photo = photo,
                            displayedPhotos = uiState.displayedPhotos,
                            daysCardPhotos = uiState.daysCardPhotos,
                            monthsCardPhotos = uiState.monthsCardPhotos,
                        )
                },
                onChangeCameraUploadsPermissions = {
                    val cameraUploadsPermissions = getCameraUploadsPermissions()
                    cameraUploadsPermissionsLauncher.launch(cameraUploadsPermissions)
                },
                onRequestNotificationPermission = {
                    if (activity == null) return@TimelineTabContent
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (isNotificationPermissionPermanentlyDenied) {
                            activity.openNotificationSettings()
                        } else {
                            notificationPermissionLauncher.launch(POST_NOTIFICATIONS)
                        }
                    }
                },
                onCUBannerDismissRequest = onCUBannerDismissRequest,
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
    cuStatusUiState: CUStatusUiState,
    selectedPhotoIds: Set<Long>,
    lazyGridState: LazyGridState,
    lazyListState: LazyListState,
    selectedTimePeriod: MediaTimePeriod,
    shouldShowTimePeriodSelector: Boolean,
    onMediaTimePeriodSelected: (MediaTimePeriod) -> Unit,
    onGridSizeChange: (value: TimelineGridSize) -> Unit,
    onPhotoClick: (node: PhotoNodeUiState) -> Unit,
    onPhotoSelected: (node: PhotoNodeUiState) -> Unit,
    onPhotosNodeListCardClick: (photo: PhotosNodeListCard) -> Unit,
    onChangeCameraUploadsPermissions: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onCUBannerDismissRequest: (status: CUStatusUiState) -> Unit,
    onNavigateToCameraUploadsSettings: () -> Unit,
    onNavigateMobileDataSetting: () -> Unit,
    onNavigateToUpgradeScreen: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Box(
        modifier = modifier.padding(
            start = contentPadding.calculateStartPadding(LayoutDirection.Ltr),
            end = contentPadding.calculateEndPadding(LayoutDirection.Ltr),
        )
    ) {
        when (selectedTimePeriod) {
            MediaTimePeriod.All -> {
                PhotosNodeGridView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TIMELINE_TAB_CONTENT_GRID_VIEW_TAG),
                    lazyGridState = lazyGridState,
                    contentPadding = PaddingValues(
                        bottom = if (selectedPhotoIds.isEmpty()) {
                            contentPadding.calculateBottomPadding() + 90.dp
                        } else {
                            contentPadding.calculateBottomPadding()
                        }
                    ),
                    items = uiState.displayedPhotos,
                    selectedPhotoIds = selectedPhotoIds,
                    gridSize = uiState.gridSize,
                    onGridSizeChange = onGridSizeChange,
                    onClick = onPhotoClick,
                    onLongClick = onPhotoSelected,
                    header = {
                        if (selectedPhotoIds.isEmpty()) {
                            CameraUploadsBanner(
                                status = cuStatusUiState,
                                onEnableCameraUploads = onNavigateToCameraUploadsSettings,
                                onDismissRequest = onCUBannerDismissRequest,
                                onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions,
                                onRequestNotificationPermission = onRequestNotificationPermission,
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
                    MediaTimePeriod.Years -> uiState.yearsCardPhotos
                    MediaTimePeriod.Months -> uiState.monthsCardPhotos
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
                        if (selectedPhotoIds.isEmpty()) {
                            CameraUploadsBanner(
                                status = cuStatusUiState,
                                onEnableCameraUploads = onNavigateToCameraUploadsSettings,
                                onDismissRequest = onCUBannerDismissRequest,
                                onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions,
                                onRequestNotificationPermission = onRequestNotificationPermission,
                                onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings,
                                onNavigateMobileDataSetting = onNavigateMobileDataSetting,
                                onNavigateUpgradeScreen = onNavigateToUpgradeScreen,
                            )
                        }
                    }
                )
            }
        }

        if (selectedPhotoIds.isEmpty()) {
            MediaTimePeriodSelector(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .align(Alignment.BottomCenter)
                    .testTag(TIMELINE_TAB_CONTENT_MEDIA_TIME_PERIOD_SELECTOR_TAG),
                isVisible = shouldShowTimePeriodSelector,
                selectedTimePeriod = selectedTimePeriod,
                onMediaTimePeriodSelected = onMediaTimePeriodSelected
            )
        }
    }
}

private fun getCameraUploadsPermissions() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            getImagePermissionByVersion(),
            getVideoPermissionByVersion(),
            READ_MEDIA_VISUAL_USER_SELECTED,
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
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

private fun trackTimePeriodSelection(timePeriod: MediaTimePeriod) {
    when (timePeriod) {
        MediaTimePeriod.Years -> Analytics.tracker.trackEvent(
            MediaScreenYearsFilterSelectedEvent
        )

        MediaTimePeriod.Months -> Analytics.tracker.trackEvent(
            MediaScreenMonthsFilterSelectedEvent
        )

        MediaTimePeriod.Days -> Analytics.tracker.trackEvent(
            MediaScreenDaysFilterSelectedEvent
        )

        MediaTimePeriod.All -> Analytics.tracker.trackEvent(
            MediaScreenAllFilterSelectedEvent
        )
    }
}

@SuppressLint("QueryPermissionsNeeded")
private fun Activity?.openNotificationSettings() {
    if (this == null) return

    val notificationIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }

    val intent = if (notificationIntent.resolveActivity(packageManager) != null) {
        notificationIntent
    } else {
        // Fallback intent
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:$packageName".toUri()
        }
    }

    try {
        startActivity(intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
    } catch (e: Exception) {
        Timber.e(e, "Failed to open notification settings")
    }
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
            selectedPhotoIds = setOf(),
            showTimelineSortDialog = false,
            selectedTimePeriod = MediaTimePeriod.All,
            clearCameraUploadsCompletedMessage = {},
            setEnableCUPage = {},
            onNavigateToCameraUploadsSettings = {},
            onGridSizeChange = {},
            onSortDialogDismissed = {},
            onSortOptionChange = {},
            onPhotoClick = {},
            onPhotoSelected = {},
            handleCameraUploadsPermissionsResult = {},
            handleNotificationPermissionResult = {},
            onCUBannerDismissRequest = {},
            onNavigateToUpgradeAccount = {},
            onMediaTimePeriodSelected = {}
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
internal const val TIMELINE_TAB_CONTENT_MEDIA_TIME_PERIOD_SELECTOR_TAG =
    "timeline_tab_content:selector_media_time_period"
internal const val MEDIA_TIME_PERIOD_SELECTOR_SPACER_TAG =
    "media_time_period_selector:spacer"
