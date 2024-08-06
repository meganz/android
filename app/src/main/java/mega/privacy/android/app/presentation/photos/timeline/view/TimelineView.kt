package mega.privacy.android.app.presentation.photos.timeline.view

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.timeline.model.CameraUploadsStatus
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import mega.privacy.android.app.presentation.photos.view.CardListView
import mega.privacy.android.app.presentation.photos.view.TimeSwitchBar
import mega.privacy.android.app.presentation.photos.view.isScrolledToEnd
import mega.privacy.android.app.presentation.photos.view.isScrollingDown
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.shared.original.core.ui.theme.black
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_012
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_087
import mega.privacy.android.shared.original.core.ui.theme.teal_100
import mega.privacy.android.shared.original.core.ui.theme.teal_200
import mega.privacy.android.shared.original.core.ui.theme.teal_300
import mega.privacy.android.shared.original.core.ui.theme.white
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_012
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_038
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_087
import mega.privacy.android.shared.original.core.ui.theme.yellow_100
import mega.privacy.android.shared.original.core.ui.theme.yellow_700
import mega.privacy.android.shared.original.core.ui.theme.yellow_700_alpha_015
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

/**
 * Base Compose Timeline View
 */
@Composable
fun TimelineView(
    photoDownload: PhotoDownload,
    timelineViewState: TimelineViewState,
    lazyGridState: LazyGridState,
    onCardClick: (DateCard) -> Unit,
    onTimeBarTabSelected: (TimeBarTab) -> Unit,
    enableCUView: @Composable () -> Unit,
    photosGridView: @Composable () -> Unit,
    emptyView: @Composable () -> Unit,
    onClickCameraUploadsSync: () -> Unit,
    onClickCameraUploadsUploading: () -> Unit,
    onChangeCameraUploadsPermissions: () -> Unit,
    onCloseCameraUploadsLimitedAccess: () -> Unit,
    clearCameraUploadsMessage: () -> Unit = {},
    clearCameraUploadsChangePermissionsMessage: () -> Unit,
    clearCameraUploadsCompletedMessage: () -> Unit,
) {
    val context = LocalContext.current
    val isBarVisible by remember {
        derivedStateOf { lazyGridState.firstVisibleItemIndex == 0 }
    }
    val isScrollingDown by lazyGridState.isScrollingDown()
    val isScrolledToEnd by lazyGridState.isScrolledToEnd()
    val scaffoldState = rememberScaffoldState()
    val isLight = MaterialTheme.colors.isLight
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
                message = context.resources.getQuantityString(
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

    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { snackBarHostState ->
            SnackbarHost(
                hostState = snackBarHostState,
                snackbar = { snackBarData ->
                    Snackbar(
                        snackbarData = snackBarData,
                        actionOnNewLine = true,
                        backgroundColor = black.takeIf { isLight } ?: white,
                        actionColor = teal_200.takeIf { isLight } ?: teal_300,
                    )
                }
            )
        },
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
                            isBarVisible = isBarVisible,
                            isScrollingDown = isScrollingDown,
                            isScrolledToEnd = isScrolledToEnd,
                            photosGridView = photosGridView,
                            photoDownload = photoDownload,
                            onCardClick = onCardClick,
                            onTimeBarTabSelected = onTimeBarTabSelected,
                            onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions,
                            onCloseCameraUploadsLimitedAccess = onCloseCameraUploadsLimitedAccess,
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
    isBarVisible: Boolean,
    isScrollingDown: Boolean,
    isScrolledToEnd: Boolean,
    photosGridView: @Composable () -> Unit,
    photoDownload: PhotoDownload,
    onCardClick: (DateCard) -> Unit,
    onTimeBarTabSelected: (TimeBarTab) -> Unit,
    onChangeCameraUploadsPermissions: () -> Unit,
    onCloseCameraUploadsLimitedAccess: () -> Unit,
) {
    LaunchedEffect(
        timelineViewState.scrollStartIndex,
        timelineViewState.scrollStartOffset,
        timelineViewState.selectedTimeBarTab,
    ) {
        lazyGridState.scrollToItem(
            timelineViewState.scrollStartIndex,
            timelineViewState.scrollStartOffset
        )
    }
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
                    accountType = timelineViewState.accountType,
                    photoDownload = photoDownload,
                    onCardClick = onCardClick,
                    cardListViewHeaderView = {
                        if (timelineViewState.isCameraUploadsLimitedAccess) {
                            CameraUploadsLimitedAccess(
                                onClick = onChangeCameraUploadsPermissions,
                                onClose = onCloseCameraUploadsLimitedAccess,
                            )
                        }
                    }
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
                    isBarVisible || (!isScrollingDown && !isScrolledToEnd)
                }
            }
        }
    }
}

@Composable
fun NewEnableCameraUploadsButton(onClick: () -> Unit) {
    val isLight = MaterialTheme.colors.isLight

    Column(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cu_status),
                        contentDescription = null,
                        tint = grey_alpha_038.takeIf { isLight } ?: white_alpha_038,
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = stringResource(id = R.string.enable_cu_subtitle),
                        color = grey_alpha_087.takeIf { isLight } ?: white_alpha_087,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W400,
                        style = MaterialTheme.typography.body2,
                    )
                },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd,
                content = {
                    Text(
                        text = stringResource(id = R.string.settings_camera_upload_on),
                        modifier = Modifier.clickable { onClick() },
                        color = teal_300.takeIf { isLight } ?: teal_100,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.button,
                    )
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Divider(
                color = grey_alpha_012.takeIf { isLight } ?: white_alpha_012,
                thickness = 1.dp,
            )
        },
    )
}

@Composable
fun CameraUploadsLimitedAccess(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onClose: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(yellow_100.takeIf { isLight } ?: yellow_700_alpha_015),
        content = {
            Row(
                modifier = Modifier.padding(16.dp),
                content = {
                    Text(
                        text = "${
                            stringResource(
                                id = R.string.camera_uploads_limited_access
                            )
                        } ${
                            stringResource(
                                id = R.string.camera_uploads_change_permissions
                            )
                        }",
                        modifier = Modifier.weight(1f),
                        color = black.takeIf { isLight } ?: yellow_700,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W400,
                        lineHeight = 18.sp,
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Spacer(modifier = Modifier.height(4.dp))

                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = null,
                            modifier = Modifier
                                .size(12.dp)
                                .clickable { onClose() },
                            tint = grey_alpha_087.takeIf { isLight } ?: yellow_700,
                        )
                    }
                },
            )

            if (isLight) {
                Divider(
                    color = grey_alpha_012,
                    thickness = 1.dp,
                )
            }
        },
    )
}
