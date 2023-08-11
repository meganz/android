package mega.privacy.android.app.presentation.photos.mediadiscovery.view

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.PhotoDownloaderViewModel
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryGlobalStateViewModel
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryViewModel
import mega.privacy.android.app.presentation.photos.mediadiscovery.model.MediaDiscoveryViewState
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.timeline.view.PhotosSkeletonView
import mega.privacy.android.app.presentation.photos.view.CardListView
import mega.privacy.android.app.presentation.photos.view.EmptyView
import mega.privacy.android.app.presentation.photos.view.FilterDialog
import mega.privacy.android.app.presentation.photos.view.PhotosGridView
import mega.privacy.android.app.presentation.photos.view.SortByDialog
import mega.privacy.android.app.presentation.photos.view.TimeSwitchBar
import mega.privacy.android.app.presentation.photos.view.photosZoomGestureDetector
import mega.privacy.android.domain.entity.photos.Photo

@Composable
fun MediaDiscoveryView(
    mediaDiscoveryGlobalStateViewModel: MediaDiscoveryGlobalStateViewModel = viewModel(),
    mediaDiscoveryViewModel: MediaDiscoveryViewModel = viewModel(),
    onOKButtonClicked: () -> Unit,
    onSettingButtonClicked: () -> Unit,
    showSettingDialog: Boolean = false,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onPhotoClicked: (Photo) -> Unit,
    onPhotoLongPressed: (Photo) -> Unit,
    onCardClick: (DateCard) -> Unit,
    onTimeBarTabSelected: (TimeBarTab) -> Unit,
) {
    val mediaDiscoveryViewState by mediaDiscoveryViewModel.state.collectAsStateWithLifecycle()
    if (mediaDiscoveryViewState.shouldBack)
        Back()

    HandleSortByDialog(
        mediaDiscoveryViewState = mediaDiscoveryViewState,
        mediaDiscoveryViewModel = mediaDiscoveryViewModel,
    )

    HandleFilterDialog(
        mediaDiscoveryGlobalStateViewModel = mediaDiscoveryGlobalStateViewModel,
        mediaDiscoveryViewModel = mediaDiscoveryViewModel,
        mediaDiscoveryViewState = mediaDiscoveryViewState,
    )

    if (mediaDiscoveryViewState.loadPhotosDone) {
        if (mediaDiscoveryViewState.uiPhotoList.isNotEmpty()) {
            MDView(
                mediaDiscoveryViewState = mediaDiscoveryViewState,
                onOKButtonClicked = onOKButtonClicked,
                onSettingButtonClicked = onSettingButtonClicked,
                showSettingDialog = showSettingDialog,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                onPhotoClicked = onPhotoClicked,
                onPhotoLongPressed = onPhotoLongPressed,
                onCardClick = onCardClick,
                onTimeBarTabSelected = onTimeBarTabSelected,
            )
        } else {
            EmptyView(mediaDiscoveryViewState.currentMediaType)
        }
    } else {
        PhotosSkeletonView()
    }
}

@Composable
private fun Back() {
    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    LaunchedEffect(key1 = true) {
        onBackPressedDispatcher?.onBackPressed()
    }
}

@Composable
private fun HandleFilterDialog(
    mediaDiscoveryViewState: MediaDiscoveryViewState,
    mediaDiscoveryViewModel: MediaDiscoveryViewModel,
    mediaDiscoveryGlobalStateViewModel: MediaDiscoveryGlobalStateViewModel,
) {
    if (mediaDiscoveryViewState.showFilterDialog) {
        FilterDialog(
            onDialogDismissed = {
                mediaDiscoveryViewModel.showFilterDialog(showFilterDialog = false)
            },
            selectedOption = mediaDiscoveryViewState.currentMediaType,
            onOptionSelected = {
                mediaDiscoveryGlobalStateViewModel.storeCurrentMediaType(it)
            }
        )
    }
}

@Composable
private fun HandleSortByDialog(
    mediaDiscoveryViewState: MediaDiscoveryViewState,
    mediaDiscoveryViewModel: MediaDiscoveryViewModel,
) {
    if (mediaDiscoveryViewState.showSortByDialog) {
        SortByDialog(
            onDialogDismissed = {
                mediaDiscoveryViewModel.showSortByDialog(showSortByDialog = false)
            },
            selectedOption = mediaDiscoveryViewState.currentSort,
            onOptionSelected = {
                mediaDiscoveryViewModel.setCurrentSort(it)
            }
        )
    }
}

@Composable
private fun MDView(
    mediaDiscoveryViewState: MediaDiscoveryViewState,
    onOKButtonClicked: () -> Unit,
    onSettingButtonClicked: () -> Unit,
    showSettingDialog: Boolean = false,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onPhotoClicked: (Photo) -> Unit,
    onPhotoLongPressed: (Photo) -> Unit,
    onCardClick: (DateCard) -> Unit,
    onTimeBarTabSelected: (TimeBarTab) -> Unit,
    photoDownloaderViewModel: PhotoDownloaderViewModel = viewModel(),
) {
    val lazyGridState: LazyGridState =
        rememberSaveable(
            mediaDiscoveryViewState.scrollStartIndex,
            mediaDiscoveryViewState.scrollStartOffset,
            saver = LazyGridState.Saver,
        ) {
            LazyGridState(
                mediaDiscoveryViewState.scrollStartIndex,
                mediaDiscoveryViewState.scrollStartOffset,
            )
        }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (showSettingDialog) {
                MediaDiscoveryDialog(
                    onOKButtonClicked = onOKButtonClicked,
                    onSettingButtonClicked = onSettingButtonClicked,
                )
            }
            if (mediaDiscoveryViewState.selectedTimeBarTab == TimeBarTab.All) {
                PhotosGridView(
                    modifier = Modifier
                        .photosZoomGestureDetector(
                            onZoomIn = onZoomIn,
                            onZoomOut = onZoomOut,
                        ),
                    currentZoomLevel = mediaDiscoveryViewState.currentZoomLevel,
                    photoDownland = photoDownloaderViewModel::downloadPhoto,
                    lazyGridState = lazyGridState,
                    onClick = onPhotoClicked,
                    onLongPress = onPhotoLongPressed,
                    selectedPhotoIds = mediaDiscoveryViewState.selectedPhotoIds,
                    uiPhotoList = mediaDiscoveryViewState.uiPhotoList,
                )
            } else {
                val dateCards = when (mediaDiscoveryViewState.selectedTimeBarTab) {
                    TimeBarTab.Years -> mediaDiscoveryViewState.yearsCardList
                    TimeBarTab.Months -> mediaDiscoveryViewState.monthsCardList
                    TimeBarTab.Days -> mediaDiscoveryViewState.daysCardList
                    else -> mediaDiscoveryViewState.daysCardList
                }
                CardListView(
                    dateCards = dateCards,
                    photoDownload = photoDownloaderViewModel::downloadPhoto,
                    onCardClick = onCardClick,
                    state = lazyGridState,
                )
            }
        }
        if (mediaDiscoveryViewState.selectedPhotoIds.isEmpty()) {
            TimeSwitchBar(
                selectedTimeBarTab = mediaDiscoveryViewState.selectedTimeBarTab,
                onTimeBarTabSelected = onTimeBarTabSelected
            )
        }
    }
}

/**
 * Media discovery view dialog
 */
@Composable
fun MediaDiscoveryDialog(
    onOKButtonClicked: () -> Unit,
    onSettingButtonClicked: () -> Unit,
) {
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 3.dp)
            .shadow(
                elevation = 10.dp,
                ambientColor = colorResource(id = R.color.black),
                spotColor = colorResource(id = R.color.black)
            )
            .zIndex(2f),
        color = Color.Transparent
    )
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 30.dp, end = 20.dp),
        text = stringResource(R.string.cloud_drive_media_discovery_banner_context),
        color = colorResource(id = R.color.grey_alpha_087_white)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        SettingsButton(
            onSettingButtonClicked = onSettingButtonClicked
        )
        OKButton(
            onOKButtonClicked = onOKButtonClicked
        )
    }
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        color = colorResource(id = R.color.grey_012_white_015)
    )
}

/**
 * Settings button for media discovery view dialog
 */
@Composable
fun SettingsButton(
    onSettingButtonClicked: () -> Unit,
) {
    TextButton(
        modifier = Modifier.padding(end = 10.dp),
        colors = ButtonDefaults.buttonColors(
            contentColor = MaterialTheme.colors.secondary,
            backgroundColor = Color.Transparent
        ),
        onClick = onSettingButtonClicked
    ) {
        Text(
            text = stringResource(R.string.cloud_drive_media_discovery_banner_settings),
            fontSize = 16.sp
        )
    }
}

/**
 * Ok button for media discovery view dialog
 */
@Composable
fun OKButton(
    onOKButtonClicked: () -> Unit,
) {
    TextButton(
        modifier = Modifier.padding(end = 8.dp),
        colors = ButtonDefaults.buttonColors(
            contentColor = MaterialTheme.colors.secondary,
            backgroundColor = Color.Transparent
        ),
        onClick = onOKButtonClicked
    ) {
        Text(
            text = stringResource(R.string.cloud_drive_media_discovery_banner_ok),
            fontSize = 16.sp
        )
    }
}