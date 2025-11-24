package mega.privacy.android.feature.photos.presentation.timeline

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
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
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
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
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.presentation.MediaCameraUploadUiState
import mega.privacy.android.feature.photos.presentation.MediaFilterUiState
import mega.privacy.android.feature.photos.presentation.component.PhotosNodeGridView
import mega.privacy.android.feature.photos.presentation.timeline.component.EnableCameraUploadsContent
import mega.privacy.android.feature.photos.presentation.timeline.component.PhotosNodeListCardListView
import mega.privacy.android.feature.photos.presentation.timeline.component.PhotosSkeletonView
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotoModificationTimePeriod
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun TimelineTabRoute(
    uiState: TimelineTabUiState,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    mediaFilterUiState: MediaFilterUiState,
    clearCameraUploadsMessage: () -> Unit,
    clearCameraUploadsCompletedMessage: () -> Unit,
    onChangeCameraUploadsPermissions: () -> Unit,
    clearCameraUploadsChangePermissionsMessage: () -> Unit,
    loadNextPage: () -> Unit,
    onNavigateCameraUploadsSettings: () -> Unit,
    setEnableCUPage: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    TimelineTabScreen(
        modifier = modifier,
        uiState = uiState,
        mediaFilterUiState = mediaFilterUiState,
        mediaCameraUploadUiState = mediaCameraUploadUiState,
        clearCameraUploadsMessage = clearCameraUploadsMessage,
        clearCameraUploadsCompletedMessage = clearCameraUploadsCompletedMessage,
        onChangeCameraUploadsPermissions = onChangeCameraUploadsPermissions,
        clearCameraUploadsChangePermissionsMessage = clearCameraUploadsChangePermissionsMessage,
        loadNextPage = loadNextPage,
        onNavigateCameraUploadsSettings = onNavigateCameraUploadsSettings,
        setEnableCUPage = setEnableCUPage,
    )
}

@Composable
internal fun TimelineTabScreen(
    uiState: TimelineTabUiState,
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    mediaFilterUiState: MediaFilterUiState,
    clearCameraUploadsMessage: () -> Unit,
    clearCameraUploadsCompletedMessage: () -> Unit,
    onChangeCameraUploadsPermissions: () -> Unit,
    clearCameraUploadsChangePermissionsMessage: () -> Unit,
    loadNextPage: () -> Unit,
    onNavigateCameraUploadsSettings: () -> Unit,
    setEnableCUPage: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
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
    var selectedTimePeriod by rememberSaveable { mutableStateOf(PhotoModificationTimePeriod.All) }
    val shouldShowTimePeriodSelector by remember(selectedTimePeriod) {
        derivedStateOf {
            if (selectedTimePeriod == PhotoModificationTimePeriod.All) {
                (!isGridScrollingDown && !isGridScrolledToEnd) || isGridScrolledToTop
            } else (!isListScrollingDown && !isListScrolledToEnd) || isListScrolledToTop
        }
    }
    val showEnableCUPage = mediaCameraUploadUiState.enableCameraUploadPageShowing
            && mediaFilterUiState.mediaSource != FilterMediaSource.CloudDrive

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

    LaunchedEffect(mediaCameraUploadUiState.showCameraUploadsChangePermissionsMessage) {
        if (mediaCameraUploadUiState.showCameraUploadsChangePermissionsMessage) {
            snackBarHostState?.showAutoDurationSnackbar(
                message = context.getString(sharedR.string.timeline_tab_camera_uploads_limited_access),
                actionLabel = context.getString(sharedR.string.timeline_tab_file_properties_shared_folder_change_permissions),
            )?.let {
                when (it) {
                    SnackbarResult.Dismissed -> Unit
                    SnackbarResult.ActionPerformed -> onChangeCameraUploadsPermissions()
                }
            }
            clearCameraUploadsChangePermissionsMessage()
        }
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

    when {
        showEnableCUPage -> {
            EnableCameraUploadsContent(
                modifier = modifier.padding(horizontal = 16.dp),
                onEnable = onNavigateCameraUploadsSettings
            )
        }

        uiState.isLoading -> {
            PhotosSkeletonView()
        }

        uiState.displayedPhotos.isEmpty() -> {
            EmptyBody(
                enableCameraUploadButtonShowing = mediaCameraUploadUiState.enableCameraUploadButtonShowing,
                mediaSource = mediaFilterUiState.mediaSource,
                setEnableCUPage = setEnableCUPage,
            )
        }

        else -> {
            TimelineTabContent(
                modifier = modifier,
                uiState = uiState,
                lazyGridState = lazyGridState,
                lazyListState = lazyListState,
                selectedTimePeriod = selectedTimePeriod,
                shouldShowTimePeriodSelector = shouldShowTimePeriodSelector,
                onPhotoTimePeriodSelected = { selectedTimePeriod = it },
            )
        }
    }
}

@Composable
private fun EmptyBody(
    mediaSource: FilterMediaSource,
    enableCameraUploadButtonShowing: Boolean,
    setEnableCUPage: (Boolean) -> Unit,
) {
    if (enableCameraUploadButtonShowing && mediaSource != FilterMediaSource.CloudDrive) {
        setEnableCUPage(true)
    } else {
        EmptyBodyContent(
            modifier = Modifier
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
    lazyGridState: LazyGridState,
    lazyListState: LazyListState,
    selectedTimePeriod: PhotoModificationTimePeriod,
    shouldShowTimePeriodSelector: Boolean,
    onPhotoTimePeriodSelected: (PhotoModificationTimePeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (selectedTimePeriod) {
            PhotoModificationTimePeriod.All -> {
                PhotosNodeGridView(
                    modifier = Modifier.fillMaxSize(),
                    lazyGridState = lazyGridState,
                    items = uiState.displayedPhotos,
                    zoomLevel = uiState.zoomLevel,
                    onZoomIn = { },
                    onZoomOut = { },
                    onClick = { },
                    onLongClick = { },
                )
            }

            else -> {
                val items = when (selectedTimePeriod) {
                    PhotoModificationTimePeriod.Years -> uiState.yearsCardPhotos
                    PhotoModificationTimePeriod.Months -> uiState.monthsCardPhotos
                    else -> uiState.daysCardPhotos
                }
                PhotosNodeListCardListView(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                    contentPadding = PaddingValues(bottom = 50.dp),
                    photos = items,
                    onClick = { }
                )
            }
        }

        if (uiState.selectedPhotoCount == 0) {
            PhotoModificationTimePeriodSelector(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
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
                    Spacer(modifier = Modifier.width(8.dp))
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

@CombinedThemePreviews
@Composable
private fun TimelineTabScreenPreview() {
    AndroidThemeForPreviews {
        TimelineTabScreen(
            modifier = Modifier.fillMaxSize(),
            uiState = TimelineTabUiState(isLoading = false),
            mediaCameraUploadUiState = MediaCameraUploadUiState(),
            mediaFilterUiState = MediaFilterUiState(),
            clearCameraUploadsMessage = {},
            clearCameraUploadsCompletedMessage = {},
            onChangeCameraUploadsPermissions = {},
            clearCameraUploadsChangePermissionsMessage = {},
            loadNextPage = {},
            setEnableCUPage = {},
            onNavigateCameraUploadsSettings = {},
        )
    }
}
