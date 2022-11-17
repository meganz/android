package mega.privacy.android.app.presentation.photos.albums.photosselection

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.ALL_PHOTOS
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.CAMERA_UPLOAD
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource.CLOUD_DRIVE
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.presentation.theme.black
import mega.privacy.android.presentation.theme.teal_300
import mega.privacy.android.presentation.theme.white

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AlbumPhotosSelectionScreen(
    viewModel: AlbumPhotosSelectionViewModel = viewModel(),
    onBackClicked: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showSelectLocationDialog by rememberSaveable { mutableStateOf(false) }
    var showMoreMenu by rememberSaveable { mutableStateOf(false) }

    handleSelectLocationDialog(
        showSelectLocationDialog = showSelectLocationDialog,
        selectedLocation = state.selectedLocation,
        onLocationSelected = { location ->
            showSelectLocationDialog = false
            viewModel.updateLocation(location)
        },
        onDialogDismissed = { showSelectLocationDialog = false },
    )

    Scaffold(
        topBar = {
            AlbumPhotosSelectionHeader(
                album = state.album,
                selectedLocation = state.selectedLocation,
                numSelectedPhotos = state.selectedPhotoIds.size,
                showFilterMenu = state.showFilterMenu,
                showMoreMenu = showMoreMenu,
                onBackClicked = onBackClicked,
                onFilterClicked = { showSelectLocationDialog = true },
                onMoreClicked = { showMoreMenu = true },
                onMoreDismissed = { showMoreMenu = false },
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
        floatingActionButton = { TODO() },
        content = { TODO() },
    )
}

@Composable
private fun AlbumPhotosSelectionHeader(
    album: Album.UserAlbum?,
    selectedLocation: TimelinePhotosSource,
    numSelectedPhotos: Int,
    showFilterMenu: Boolean,
    showMoreMenu: Boolean,
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
                        text = album?.title.orEmpty(),
                        fontWeight = FontWeight.W500,
                        style = MaterialTheme.typography.subtitle1,
                    )
                    Text(
                        text = selectedLocation.text(),
                        modifier = Modifier.alpha(0.54f),
                        style = MaterialTheme.typography.caption,
                    )
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
                    DropdownMenuItem(onClick = onSelectAllClicked) {
                        Text(text = stringResource(id = R.string.action_select_all))
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
private fun SelectLocationDialog(
    selectedLocation: TimelinePhotosSource,
    onLocationSelected: (TimelinePhotosSource) -> Unit,
    onDialogDismissed: () -> Unit,
) {
    Dialog(onDismissRequest = onDialogDismissed) {
        Card(shape = RoundedCornerShape(2.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.size(12.dp))
                TimelinePhotosSource.values().forEach { location ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLocationSelected(location) }
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
                            modifier = Modifier.alpha(
                                1f.takeIf { location == selectedLocation } ?: 0.54f
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
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
private fun TimelinePhotosSource.text(): String = when (this) {
    ALL_PHOTOS -> stringResource(id = R.string.filter_button_all_source)
    CLOUD_DRIVE -> stringResource(id = R.string.filter_button_cd_only)
    CAMERA_UPLOAD -> stringResource(id = R.string.photos_filter_camera_uploads)
}
