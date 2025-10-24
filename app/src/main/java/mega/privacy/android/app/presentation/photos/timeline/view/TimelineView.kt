package mega.privacy.android.app.presentation.photos.timeline.view

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.timeline.model.CameraUploadsBannerType
import mega.privacy.android.app.presentation.photos.timeline.model.CameraUploadsStatus
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import mega.privacy.android.app.presentation.photos.view.CardListView
import mega.privacy.android.app.presentation.photos.view.TimeSwitchBar
import mega.privacy.android.app.presentation.photos.view.isScrolledToEnd
import mega.privacy.android.app.presentation.photos.view.isScrolledToTop
import mega.privacy.android.app.presentation.photos.view.isScrollingDown
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar


/**
 * Base Compose Timeline View
 */
@Composable
fun TimelineView(
    photoDownload: PhotoDownload,
    timelineViewState: TimelineViewState,
    lazyGridState: LazyGridState,
    onCardClick: (DateCard) -> Unit = {},
    onTimeBarTabSelected: (TimeBarTab) -> Unit = {},
    enableCUView: @Composable () -> Unit = {},
    photosGridView: @Composable () -> Unit = {},
    emptyView: @Composable () -> Unit = {},
    onClickCameraUploadsSync: () -> Unit = {},
    onClickCameraUploadsUploading: () -> Unit = {},
    onChangeCameraUploadsPermissions: () -> Unit = {},
    clearCameraUploadsMessage: () -> Unit = {},
    clearCameraUploadsChangePermissionsMessage: () -> Unit = {},
    clearCameraUploadsCompletedMessage: () -> Unit = {},
    loadPhotos: () -> Unit = {},
    cameraUploadsBanners: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val resource = LocalResources.current
    val isScrollingDown by lazyGridState.isScrollingDown()
    val isScrolledToEnd by lazyGridState.isScrolledToEnd()
    val isScrolledToTop by lazyGridState.isScrolledToTop()
    val scaffoldState = rememberScaffoldState()
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val showEnableCUPage = timelineViewState.enableCameraUploadPageShowing
            && timelineViewState.currentMediaSource != TimelinePhotosSource.CLOUD_DRIVE
    val scrollNotInProgress by remember {
        derivedStateOf { !lazyGridState.isScrollInProgress }
    }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        if (message.isNotEmpty()) {
            scaffoldState.snackbarHostState.showAutoDurationSnackbar(message)
            message = ""
        }
    }

    LaunchedEffect(timelineViewState.cameraUploadsMessage) {
        if (timelineViewState.cameraUploadsMessage.isNotEmpty()) {
            scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                message = timelineViewState.cameraUploadsMessage,
            )
            clearCameraUploadsMessage()
        }
    }

    LaunchedEffect(timelineViewState.showCameraUploadsCompletedMessage) {
        if (timelineViewState.showCameraUploadsCompletedMessage) {
            scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                message = resource.getQuantityString(
                    R.plurals.photos_camera_uploads_completed,
                    timelineViewState.cameraUploadsTotalUploaded,
                    timelineViewState.cameraUploadsTotalUploaded,
                )
            )
        }
        clearCameraUploadsCompletedMessage()
    }

    LaunchedEffect(timelineViewState.showCameraUploadsChangePermissionsMessage) {
        if (timelineViewState.showCameraUploadsChangePermissionsMessage) {
            val result = scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                message = context.getString(R.string.camera_uploads_limited_access),
                actionLabel = context.getString(R.string.file_properties_shared_folder_change_permissions),
            )
            when (result) {
                SnackbarResult.Dismissed -> {}

                SnackbarResult.ActionPerformed -> onChangeCameraUploadsPermissions()
            }
            clearCameraUploadsChangePermissionsMessage()
        }
    }

    LaunchedEffect(isScrolledToEnd) {
        if (isScrolledToEnd) {
            loadPhotos()
        }
    }

    MegaScaffold(
        scaffoldState = scaffoldState,
        floatingActionButton = {
            Row(
                modifier = Modifier.padding(
                    bottom = if (isPortrait && timelineViewState.currentShowingPhotos.isNotEmpty())
                        52.dp else 0.dp,
                )
            ) {
                AnimatedVisibility(
                    visible = scrollNotInProgress,
                    exit = scaleOut(),
                    enter = scaleIn()
                ) {
                    Column {
                        Spacer(modifier = Modifier.size(if (isPortrait) 1.dp else 24.dp))

                        when (timelineViewState.cameraUploadsStatus) {
                            CameraUploadsStatus.Sync -> {
                                CameraUploadsStatusSync(
                                    onClick = onClickCameraUploadsSync,
                                )
                            }

                            CameraUploadsStatus.Uploading -> {
                                CameraUploadsStatusUploading(
                                    progress = timelineViewState.cameraUploadsProgress,
                                    onClick = onClickCameraUploadsUploading,
                                )
                            }

                            CameraUploadsStatus.Complete -> {
                                CameraUploadsStatusCompleted(
                                    onClick = {},
                                )
                            }

                            CameraUploadsStatus.Warning -> {
                                CameraUploadsStatusWarning(
                                    progress = timelineViewState.cameraUploadsProgress,
                                    onClick = {
                                        when (timelineViewState.cameraUploadsFinishedReason) {
                                            CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET -> {
                                                message = context.getString(
                                                    R.string.photos_camera_uploads_no_internet
                                                )
                                            }

                                            CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW -> {
                                                message = context.getString(
                                                    R.string.photos_camera_uploads_low_battery,
                                                    20,
                                                )
                                            }

                                            else -> {
                                                message = context.getString(
                                                    R.string.photos_camera_uploads_general_issue
                                                )
                                            }
                                        }
                                    },
                                )
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (showEnableCUPage) {
                enableCUView()
            } else {
                if (timelineViewState.loadPhotosDone) {
                    if (timelineViewState.currentShowingPhotos.isEmpty()) {
                        emptyView()
                    } else {
                        HandlePhotosGridView(
                            timelineViewState = timelineViewState,
                            lazyGridState = lazyGridState,
                            isScrollingDown = isScrollingDown,
                            isScrolledToEnd = isScrolledToEnd,
                            isScrolledToTop = isScrolledToTop,
                            photosGridView = photosGridView,
                            photoDownload = photoDownload,
                            onCardClick = onCardClick,
                            onTimeBarTabSelected = onTimeBarTabSelected,
                            stickyHeaderView = cameraUploadsBanners,
                        )
                    }
                } else {
                    //show skeleton view.
                    PhotosSkeletonView()
                }
            }
        }
    }
}

@Composable
private fun HandlePhotosGridView(
    timelineViewState: TimelineViewState,
    lazyGridState: LazyGridState,
    isScrollingDown: Boolean,
    isScrolledToEnd: Boolean,
    isScrolledToTop: Boolean,
    photosGridView: @Composable () -> Unit,
    photoDownload: PhotoDownload,
    onCardClick: (DateCard) -> Unit,
    onTimeBarTabSelected: (TimeBarTab) -> Unit,
    stickyHeaderView: @Composable () -> Unit = {},
) {
    // Load Photos
    Box {
        when (timelineViewState.selectedTimeBarTab) {
            TimeBarTab.All -> {
                Column {
                    photosGridView()
                }
            }

            else -> {
                val dateCards = when (timelineViewState.selectedTimeBarTab) {
                    TimeBarTab.Years -> timelineViewState.yearsCardPhotos
                    TimeBarTab.Months -> timelineViewState.monthsCardPhotos
                    TimeBarTab.Days -> timelineViewState.daysCardPhotos
                    else -> timelineViewState.daysCardPhotos
                }
                CardListView(
                    state = lazyGridState,
                    dateCards = dateCards,
                    shouldApplySensitiveMode = timelineViewState.hiddenNodeEnabled
                            && timelineViewState.accountType?.isPaid == true
                            && !timelineViewState.isBusinessAccountExpired,
                    photoDownload = photoDownload,
                    onCardClick = onCardClick,
                    cameraUploadsBanners = stickyHeaderView,
                )
            }
        }

        if (timelineViewState.selectedPhotoCount == 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd,
            ) {
                TimeSwitchBar(
                    timeBarTabs = timelineViewState.timeBarTabs,
                    onTimeBarTabSelected = onTimeBarTabSelected,
                    selectedTimeBarTab = timelineViewState.selectedTimeBarTab,
                ) {
                    (!isScrollingDown && !isScrolledToEnd) || isScrolledToTop
                }
            }
        }
    }
}

@Composable
internal fun CameraUploadsBanners(
    timelineViewState: TimelineViewState,
    bannerType: CameraUploadsBannerType,
    isWarningBannerShown: Boolean,
    isBannerShown: Boolean,
    onChangeCameraUploadsPermissions: () -> Unit,
    onEnableCameraUploads: () -> Unit,
    onNavigateToCameraUploadsTransferScreen: () -> Unit,
    onNavigateToCameraUploadsSettings: () -> Unit,
    onWarningBannerDismissed: () -> Unit,
    onNavigateMobileDataSetting: () -> Unit,
    onNavigateUpgradeScreen: () -> Unit,
) {
    val pendingCount = timelineViewState.pending
    val selectPhotoCount = timelineViewState.selectedPhotoCount

    if (selectPhotoCount > 0 || bannerType == CameraUploadsBannerType.NONE) {
        return
    }

    when (bannerType) {
        CameraUploadsBannerType.NoFullAccess,
        CameraUploadsBannerType.DeviceChargingNotMet,
        CameraUploadsBannerType.LowBattery,
        CameraUploadsBannerType.NetworkRequirementNotMet,
        CameraUploadsBannerType.FullStorage,
            -> {
            SlideBanner(visible = isWarningBannerShown) {
                CameraUploadsWarningBanner(
                    bannerType = bannerType,
                    onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions,
                    onWarningBannerDismissed = onWarningBannerDismissed,
                    onNavigateToCameraUploadsSettings = onNavigateToCameraUploadsSettings,
                    onNavigateMobileDataSetting = onNavigateMobileDataSetting,
                    onNavigateUpgradeScreen = onNavigateUpgradeScreen
                )
            }
        }

        else -> {
            SlideBanner(visible = isBannerShown) {
                CameraUploadsBanner(
                    bannerType = bannerType,
                    pendingCount = pendingCount,
                    isTransferScreenAvailable = timelineViewState.isCameraUploadsTransferScreenEnabled,
                    onEnableCameraUploads = onEnableCameraUploads,
                    onNavigateToCameraUploadsTransferScreen = {
                        if (timelineViewState.isCameraUploadsTransferScreenEnabled) {
                            onNavigateToCameraUploadsTransferScreen()
                        }
                    },
                )
            }
        }
    }
}

internal fun getCameraUploadsBannerType(
    timelineViewState: TimelineViewState,
): CameraUploadsBannerType {
    return when {
        timelineViewState.enableCameraUploadButtonShowing && timelineViewState.selectedPhotoCount == 0 ->
            CameraUploadsBannerType.EnableCameraUploads

        timelineViewState.isCUPausedWarningBannerEnabled &&
                timelineViewState.cameraUploadsFinishedReason == CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA
            -> CameraUploadsBannerType.FullStorage

        timelineViewState.isCUPausedWarningBannerEnabled &&
                timelineViewState.cameraUploadsFinishedReason == CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET
            -> CameraUploadsBannerType.NetworkRequirementNotMet

        timelineViewState.isCUPausedWarningBannerEnabled &&
                timelineViewState.cameraUploadsFinishedReason == CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW
            -> CameraUploadsBannerType.LowBattery

        timelineViewState.isCUPausedWarningBannerEnabled &&
                timelineViewState.cameraUploadsFinishedReason == CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET
            -> CameraUploadsBannerType.DeviceChargingNotMet

        timelineViewState.isCameraUploadsLimitedAccess -> CameraUploadsBannerType.NoFullAccess

        timelineViewState.cameraUploadsStatus == CameraUploadsStatus.Sync ->
            CameraUploadsBannerType.CheckingUploads

        timelineViewState.cameraUploadsStatus == CameraUploadsStatus.Uploading ->
            CameraUploadsBannerType.PendingCount

        else -> CameraUploadsBannerType.NONE
    }
}


@Composable
private fun CameraUploadsWarningBanner(
    bannerType: CameraUploadsBannerType,
    onChangeCameraUploadsPermissions: () -> Unit,
    onWarningBannerDismissed: () -> Unit,
    onNavigateToCameraUploadsSettings: () -> Unit,
    onNavigateMobileDataSetting: () -> Unit,
    onNavigateUpgradeScreen: () -> Unit,
) {
    val shouldAutoCloseable =
        bannerType !in listOf(
            CameraUploadsBannerType.NONE,
            CameraUploadsBannerType.FullStorage
        )

    if (shouldAutoCloseable) {
        LaunchedEffect(Unit) {
            delay(WARNING_BANNER_AUTO_HIDE_DELAY)
            onWarningBannerDismissed()
        }
    }

    when (bannerType) {
        CameraUploadsBannerType.NetworkRequirementNotMet ->
            NetworkRequirementNotMetPausedBanner(onNavigateMobileDataSetting = onNavigateMobileDataSetting)

        CameraUploadsBannerType.FullStorage ->
            FullStorageBanner(onUpgradeClicked = onNavigateUpgradeScreen)

        CameraUploadsBannerType.NoFullAccess -> {
            CameraUploadsNoFullAccessBanner(
                onClick = onChangeCameraUploadsPermissions,
                onClose = onWarningBannerDismissed,
            )
        }

        CameraUploadsBannerType.DeviceChargingNotMet ->
            DeviceChargingNotMetPausedBanner(onOpenSettingsClicked = onNavigateToCameraUploadsSettings)

        CameraUploadsBannerType.LowBattery -> LowBatteryPausedBanner()
        else -> {}
    }
}

@Composable
private fun CameraUploadsBanner(
    bannerType: CameraUploadsBannerType,
    pendingCount: Int,
    isTransferScreenAvailable: Boolean,
    onEnableCameraUploads: () -> Unit,
    onNavigateToCameraUploadsTransferScreen: () -> Unit,
) {
    when (bannerType) {
        CameraUploadsBannerType.EnableCameraUploads ->
            EnableCameraUploadsBanner(onClick = onEnableCameraUploads)

        CameraUploadsBannerType.CheckingUploads -> CameraUploadsCheckingUploadsBanner()
        CameraUploadsBannerType.PendingCount ->
            CameraUploadsPendingCountBanner(
                count = pendingCount,
                isTransferScreenAvailable = isTransferScreenAvailable,
                onClick = onNavigateToCameraUploadsTransferScreen
            )

        else -> {}
    }
}

@Composable
private fun SlideBanner(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    val animationDuration = 300
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(animationDuration),
            initialOffsetY = { -it }
        ) + fadeIn(
            animationSpec = tween(animationDuration)
        ),
        exit = slideOutVertically(
            animationSpec = tween(animationDuration),
            targetOffsetY = { -it }
        ) + fadeOut(
            animationSpec = tween(animationDuration)
        )
    ) {
        content()
    }
}

private const val WARNING_BANNER_AUTO_HIDE_DELAY = 5000L
