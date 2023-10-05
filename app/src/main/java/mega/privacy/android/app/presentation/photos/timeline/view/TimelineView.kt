package mega.privacy.android.app.presentation.photos.timeline.view

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.timeline.CUStatusFabs
import mega.privacy.android.app.presentation.photos.timeline.model.ApplyFilterMediaType
import mega.privacy.android.app.presentation.photos.timeline.model.CameraUploadsStatus
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource
import mega.privacy.android.app.presentation.photos.timeline.model.TimelineViewState
import mega.privacy.android.app.presentation.photos.view.CardListView
import mega.privacy.android.app.presentation.photos.view.TimeSwitchBar
import mega.privacy.android.app.presentation.photos.view.isScrolledToEnd
import mega.privacy.android.app.presentation.photos.view.isScrollingDown
import mega.privacy.android.core.ui.controls.progressindicator.MegaLinearProgressIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.grey_alpha_012
import mega.privacy.android.core.ui.theme.grey_alpha_038
import mega.privacy.android.core.ui.theme.grey_alpha_087
import mega.privacy.android.core.ui.theme.teal_100
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.core.ui.theme.white_alpha_012
import mega.privacy.android.core.ui.theme.white_alpha_038
import mega.privacy.android.core.ui.theme.white_alpha_087

/**
 * Base Compose Timeline View
 */
@Composable
fun TimelineView(
    photoDownload: PhotoDownload,
    timelineViewState: TimelineViewState,
    lazyGridState: LazyGridState,
    isNewCUEnabled: Boolean,
    onTextButtonClick: () -> Unit,
    onFilterFabClick: () -> Unit,
    onCardClick: (DateCard) -> Unit,
    onTimeBarTabSelected: (TimeBarTab) -> Unit,
    enableCUView: @Composable () -> Unit,
    photosGridView: @Composable () -> Unit,
    emptyView: @Composable () -> Unit,
    setCameraUploadsStatus: (CameraUploadsStatus) -> Unit,
    setCameraUploadsMessage: (String) -> Unit,
    clearCameraUploadsMessage: () -> Unit = {},
) {
    val isBarVisible by remember {
        derivedStateOf { lazyGridState.firstVisibleItemIndex == 0 }
    }
    val isScrollingDown by lazyGridState.isScrollingDown()
    val isScrolledToEnd by lazyGridState.isScrolledToEnd()
    val scaffoldState = rememberScaffoldState()
    val isLight = MaterialTheme.colors.isLight
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val coroutineScope = rememberCoroutineScope()
    val showEnableCUPage = timelineViewState.enableCameraUploadPageShowing
            && timelineViewState.currentMediaSource != TimelinePhotosSource.CLOUD_DRIVE
    val scrollNotInProgress by remember {
        derivedStateOf { !lazyGridState.isScrollInProgress }
    }

    if (timelineViewState.cameraUploadsMessage.isNotEmpty()) {
        LaunchedEffect(Unit) {
            scaffoldState.snackbarHostState.showSnackbar(
                message = timelineViewState.cameraUploadsMessage,
            )
            if (timelineViewState.cameraUploadsStatus == CameraUploadsStatus.Completed) {
                setCameraUploadsStatus(CameraUploadsStatus.Idle)
            }
            clearCameraUploadsMessage()
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
                        backgroundColor = black.takeIf { isLight } ?: white,
                    )
                }
            )
        },
        floatingActionButton = {
            Row(
                modifier = Modifier.padding(
                    bottom = if (isPortrait && timelineViewState.currentShowingPhotos.isNotEmpty())
                        68.dp else 16.dp,
                    end = 16.dp
                )
            ) {
                AnimatedVisibility(
                    visible = scrollNotInProgress,
                    exit = scaleOut(),
                    enter = scaleIn()
                ) {
                    if (isNewCUEnabled) {
                        if (timelineViewState.showCUStatusFabs) {
                            CUStatusFabs()
                        }
                        HandleCameraUploadStatusFab(
                            cameraUploadsStatus = timelineViewState.cameraUploadsStatus,
                            cameraUploadsProgress = timelineViewState.cameraUploadsProgress,
                            cameraUploadsPending = timelineViewState.cameraUploadsPending,
                            scaffoldState = scaffoldState,
                            coroutineScope = coroutineScope,
                            setCameraUploadsMessage = setCameraUploadsMessage,
                            cameraUploadsTotalFiles = timelineViewState.cameraUploadsTotalFiles
                        )
                    } else {
                        val showFilterFab =
                            timelineViewState.applyFilterMediaType != ApplyFilterMediaType.ALL_MEDIA_IN_CD_AND_CU
                                    && !showEnableCUPage
                        if (showFilterFab) {
                            HandleFilterFab(onFilterFabClick = onFilterFabClick)
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
                            onTextButtonClick = onTextButtonClick,
                            isBarVisible = isBarVisible,
                            isScrollingDown = isScrollingDown,
                            isScrolledToEnd = isScrolledToEnd,
                            photosGridView = photosGridView,
                            photoDownload = photoDownload,
                            onCardClick = onCardClick,
                            onTimeBarTabSelected = onTimeBarTabSelected,
                            isNewCUEnabled = isNewCUEnabled,
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
private fun HandleFilterFab(
    onFilterFabClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onFilterFabClick,
        modifier = Modifier
            .size(40.dp)
            .padding(all = 0.dp)
    ) {
        Icon(
            painter = painterResource(
                id = R.drawable.ic_filter
            ),
            contentDescription = "Exit filter",
            tint = if (!MaterialTheme.colors.isLight) {
                Color.Black
            } else {
                Color.White
            }
        )
    }
}

@Composable
private fun HandlePhotosGridView(
    timelineViewState: TimelineViewState,
    lazyGridState: LazyGridState,
    onTextButtonClick: () -> Unit,
    isBarVisible: Boolean,
    isScrollingDown: Boolean,
    isScrolledToEnd: Boolean,
    isNewCUEnabled: Boolean,
    photosGridView: @Composable () -> Unit,
    photoDownload: PhotoDownload,
    onCardClick: (DateCard) -> Unit,
    onTimeBarTabSelected: (TimeBarTab) -> Unit,
) {
    LaunchedEffect(
        timelineViewState.scrollStartIndex,
        timelineViewState.scrollStartOffset
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
                    if (timelineViewState.enableCameraUploadButtonShowing && timelineViewState.selectedPhotoCount == 0) {
                        if (!isNewCUEnabled) {
                            EnableCameraUploadsButton(onClick = onTextButtonClick) {
                                isBarVisible || (!isScrollingDown && !isScrolledToEnd)
                            }
                        }
                    }

                    if (timelineViewState.progressBarShowing) {
                        CameraUploadProgressBar(timelineViewState = timelineViewState) {
                            isBarVisible || (!isScrollingDown && !isScrolledToEnd)
                        }
                    }

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
                    photoDownload = photoDownload,
                    onCardClick = onCardClick,
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

/**
 * Camera Upload Progress Bar
 */
@Composable
fun CameraUploadProgressBar(
    timelineViewState: TimelineViewState = TimelineViewState(),
    isVisible: () -> Boolean = { true },
) {
    AnimatedVisibility(
        visible = isVisible(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = LocalContext.current.resources.getQuantityString(
                        R.plurals.cu_upload_progress,
                        timelineViewState.pending,
                        timelineViewState.pending
                    ),
                    color = colorResource(id = R.color.grey_087_white_087),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
                )

                MegaLinearProgressIndicator(
                    progress = timelineViewState.progress
                )
            }
        }
    }
}

/**
 * Enable Camera Upload button for when it is disabled
 */
@Composable
private fun EnableCameraUploadsButton(onClick: () -> Unit, isVisible: () -> Boolean) {
    AnimatedVisibility(
        visible = isVisible(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                ),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp
                ),
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RectangleShape
            ) {
                Text(
                    text = stringResource(id = R.string.settings_camera_upload_on),
                    color = colorResource(id = R.color.teal_300),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
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
fun HandleCameraUploadStatusFab(
    cameraUploadsStatus: CameraUploadsStatus,
    cameraUploadsProgress: Float,
    cameraUploadsPending: Int,
    cameraUploadsTotalFiles: Int,
    scaffoldState: ScaffoldState,
    coroutineScope: CoroutineScope,
    setCameraUploadsMessage: (String) -> Unit,
) {
    when (cameraUploadsStatus) {
        CameraUploadsStatus.None -> {}
        CameraUploadsStatus.Sync -> CameraUploadsStatusSync()
        CameraUploadsStatus.Uploading -> {
            val snackBarMessage = pluralStringResource(
                id = R.plurals.camera_uploads_in_progress_pending_files,
                count = cameraUploadsPending,
                cameraUploadsPending
            )
            CameraUploadsStatusUploading(
                progress = cameraUploadsProgress,
                onClick = {
                    coroutineScope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = snackBarMessage,
                        )
                    }
                }
            )
        }

        CameraUploadsStatus.Completed -> {
            val snackBarMessage = pluralStringResource(
                id = R.plurals.camera_uploads_complete_uploaded_files,
                count = cameraUploadsTotalFiles,
                cameraUploadsTotalFiles,
            )
            CameraUploadsStatusCompleted()
            setCameraUploadsMessage(snackBarMessage)
        }

        CameraUploadsStatus.Idle -> {}

        CameraUploadsStatus.Warning -> {
            val snackBarMessage =
                stringResource(id = R.string.camera_uploads_paused_due_to_no_internet)
            CameraUploadsStatusWarning(
                progress = cameraUploadsProgress,
                onClick = {
                    coroutineScope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = snackBarMessage,
                        )
                    }
                }
            )
        }

        CameraUploadsStatus.Error -> {}
    }
}

/**
 * CU Progress Bar Preview
 */
@CombinedThemePreviews()
@Composable
fun PreviewProgressBar() {
    AndroidTheme(isSystemInDarkTheme()) {
        CameraUploadProgressBar(
            timelineViewState = TimelineViewState(
                progressBarShowing = true,
                pending = 50,
                progress = 0.5f,
            )
        )
    }
}

