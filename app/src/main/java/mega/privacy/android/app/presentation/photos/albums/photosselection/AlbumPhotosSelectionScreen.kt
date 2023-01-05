package mega.privacy.android.app.presentation.photos.albums.photosselection

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.UIPhoto
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.ALL_PHOTOS
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.CAMERA_UPLOAD
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.CLOUD_DRIVE
import mega.privacy.android.app.presentation.photos.view.PhotosGridView
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.presentation.theme.black
import mega.privacy.android.presentation.theme.grey_alpha_054
import mega.privacy.android.presentation.theme.grey_alpha_087
import mega.privacy.android.presentation.theme.teal_300
import mega.privacy.android.presentation.theme.white
import mega.privacy.android.presentation.theme.white_alpha_054
import mega.privacy.android.presentation.theme.white_alpha_087

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AlbumPhotosSelectionScreen(
    viewModel: AlbumPhotosSelectionViewModel = viewModel(),
    onBackClicked: () -> Unit,
    onCompletion: (albumId: AlbumId, message: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLight = MaterialTheme.colors.isLight
    val lazyGridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    var showSelectLocationDialog by rememberSaveable { mutableStateOf(false) }
    var showMoreMenu by rememberSaveable { mutableStateOf(false) }

    if (state.isInvalidAlbum) onBackClicked()

    handleSelectLocationDialog(
        showSelectLocationDialog = showSelectLocationDialog,
        selectedLocation = state.selectedLocation,
        onLocationSelected = { location ->
            showSelectLocationDialog = false
            coroutineScope.launch { lazyGridState.scrollToItem(0) }

            viewModel.updateLocation(location)
            viewModel.filterPhotos()
        },
        onDialogDismissed = {
            showSelectLocationDialog = false
        },
    )

    handleAddPhotosCompletion(
        album = state.album,
        isSelectionCompleted = state.isSelectionCompleted,
        numCommittedPhotos = state.numCommittedPhotos,
        onCompletion = onCompletion,
    )

    Scaffold(
        topBar = {
            AlbumPhotosSelectionHeader(
                album = state.album,
                selectedLocation = state.selectedLocation,
                isLocationDetermined = state.isLocationDetermined,
                numSelectedPhotos = state.selectedPhotoIds.size,
                showFilterMenu = state.showFilterMenu,
                showMoreMenu = showMoreMenu,
                showSelectAllMenu = (state.filteredPhotoIds - state.selectedPhotoIds).isNotEmpty(),
                onBackClicked = {
                    if (state.selectedPhotoIds.isEmpty()) {
                        onBackClicked()
                    } else {
                        viewModel.clearSelection()
                    }
                },
                onFilterClicked = {
                    showSelectLocationDialog = true
                },
                onMoreClicked = {
                    showMoreMenu = true
                },
                onMoreDismissed = {
                    showMoreMenu = false
                },
                onSelectAllClicked = {
                    showMoreMenu = false
                    viewModel.selectAllPhotos()
                },
                onClearSelectionClicked = {
                    showMoreMenu = false
                    viewModel.clearSelection()
                },
            )
        },
        floatingActionButton = {
            val album = state.album
            val photos = state.photos

            if (album != null && photos.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        viewModel.addPhotos(
                            album = album,
                            selectedPhotoIds = state.selectedPhotoIds,
                        )
                    },
                    backgroundColor = teal_300,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = null,
                        tint = white.takeIf { isLight } ?: black,
                    )
                }
            }
        },
        content = {
            AlbumPhotosSelectionContent(
                lazyGridState = lazyGridState,
                uiPhotos = state.uiPhotos,
                selectedPhotoIds = state.selectedPhotoIds,
                onPhotoDownload = viewModel::downloadPhoto,
                onPhotoSelection = { photo ->
                    if (photo.id in state.selectedPhotoIds) {
                        viewModel.unselectPhoto(photo)
                    } else {
                        viewModel.selectPhoto(photo)
                    }
                },
            )
        },
    )
}

@Composable
private fun AlbumPhotosSelectionHeader(
    album: Album.UserAlbum?,
    selectedLocation: TimelinePhotosSource,
    isLocationDetermined: Boolean,
    numSelectedPhotos: Int,
    showFilterMenu: Boolean,
    showMoreMenu: Boolean,
    showSelectAllMenu: Boolean,
    onBackClicked: () -> Unit,
    onFilterClicked: () -> Unit,
    onMoreClicked: () -> Unit,
    onMoreDismissed: () -> Unit,
    onSelectAllClicked: () -> Unit,
    onClearSelectionClicked: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    TopAppBar(
        title = {
            Column {
                if (numSelectedPhotos > 0) {
                    Text(
                        text = "$numSelectedPhotos",
                        color = teal_300,
                        fontWeight = FontWeight.W500,
                        style = MaterialTheme.typography.subtitle1,
                    )
                } else {
                    Text(
                        text = stringResource(
                            id = R.string.photos_album_selection_title,
                            album?.title.orEmpty(),
                        ),
                        fontWeight = FontWeight.W500,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.subtitle1,
                    )

                    if (isLocationDetermined) {
                        Text(
                            text = selectedLocation.text(),
                            color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                            style = MaterialTheme.typography.caption,
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back_white),
                    contentDescription = null,
                    tint = teal_300.takeIf {
                        numSelectedPhotos > 0
                    } ?: (black.takeIf { isLight } ?: white),
                )
            }
        },
        actions = {
            if (showFilterMenu) {
                IconButton(onClick = onFilterClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_filter_light),
                        contentDescription = null,
                        tint = teal_300.takeIf {
                            numSelectedPhotos > 0
                        } ?: (black.takeIf { isLight } ?: white),
                    )
                }
            }

            if (numSelectedPhotos > 0) {
                IconButton(onClick = onMoreClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_dots_vertical_white),
                        contentDescription = null,
                        tint = teal_300,
                    )
                }

                DropdownMenu(expanded = showMoreMenu, onDismissRequest = onMoreDismissed) {
                    if (showSelectAllMenu) {
                        DropdownMenuItem(onClick = onSelectAllClicked) {
                            Text(text = stringResource(id = R.string.action_select_all))
                        }
                    }
                    DropdownMenuItem(onClick = onClearSelectionClicked) {
                        Text(text = stringResource(id = R.string.action_unselect_all))
                    }
                }
            }
        },
        elevation = 0.dp,
    )
}

@Composable
private fun AlbumPhotosSelectionContent(
    lazyGridState: LazyGridState,
    uiPhotos: List<UIPhoto>,
    selectedPhotoIds: Set<Long>,
    onPhotoDownload: PhotoDownload,
    onPhotoSelection: (Photo) -> Unit,
) {
    PhotosGridView(
        currentZoomLevel = ZoomLevel.Grid_3,
        photoDownland = { _, photo, callback ->
            onPhotoDownload(photo, callback)
        },
        lazyGridState = lazyGridState,
        onClick = onPhotoSelection,
        onLongPress = onPhotoSelection,
        selectedPhotoIds = selectedPhotoIds,
        uiPhotoList = uiPhotos,
    )
}

@Composable
private fun handleSelectLocationDialog(
    showSelectLocationDialog: Boolean,
    selectedLocation: TimelinePhotosSource,
    onLocationSelected: (TimelinePhotosSource) -> Unit,
    onDialogDismissed: () -> Unit,
) {
    if (showSelectLocationDialog) {
        SelectLocationDialog(
            selectedLocation = selectedLocation,
            onLocationSelected = onLocationSelected,
            onDialogDismissed = onDialogDismissed,
        )
    }
}

@Composable
private fun SelectLocationDialog(
    selectedLocation: TimelinePhotosSource,
    onLocationSelected: (TimelinePhotosSource) -> Unit,
    onDialogDismissed: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    Dialog(onDismissRequest = onDialogDismissed) {
        Card(shape = RoundedCornerShape(2.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.size(12.dp))
                TimelinePhotosSource.values().forEach { location ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (location == selectedLocation) {
                                    onDialogDismissed()
                                } else {
                                    onLocationSelected(location)
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = CenterVertically,
                    ) {
                        RadioButton(
                            selected = location == selectedLocation,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(
                            text = location.text(),
                            color = if (isLight) {
                                grey_alpha_087.takeIf {
                                    location == selectedLocation
                                } ?: grey_alpha_054
                            } else {
                                white_alpha_087.takeIf {
                                    location == selectedLocation
                                } ?: white_alpha_054
                            },
                            fontWeight = FontWeight.W400,
                            style = MaterialTheme.typography.subtitle2,
                        )
                    }
                    Spacer(modifier = Modifier.size(4.dp))
                }
                Text(
                    text = stringResource(id = R.string.button_cancel).uppercase(),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp, end = 16.dp, bottom = 16.dp)
                        .clickable { onDialogDismissed() },
                    color = teal_300,
                    fontWeight = FontWeight.W500,
                    style = MaterialTheme.typography.button,
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun handleAddPhotosCompletion(
    album: Album.UserAlbum?,
    isSelectionCompleted: Boolean,
    numCommittedPhotos: Int,
    onCompletion: (albumId: AlbumId, message: String) -> Unit,
) {
    val albumId = album?.id
    if (albumId != null && isSelectionCompleted) {
        val message = "".takeIf { numCommittedPhotos <= 0 } ?: pluralStringResource(
            id = R.plurals.photos_album_selection_added,
            count = numCommittedPhotos,
            numCommittedPhotos,
            album.title,
        )

        LaunchedEffect(Unit) {
            onCompletion(album.id, message)
        }
    }
}

@Composable
private fun TimelinePhotosSource.text(): String = when (this) {
    ALL_PHOTOS -> stringResource(id = R.string.filter_button_all_source)
    CLOUD_DRIVE -> stringResource(id = R.string.filter_button_cd_only)
    CAMERA_UPLOAD -> stringResource(id = R.string.photos_filter_camera_uploads)
}
