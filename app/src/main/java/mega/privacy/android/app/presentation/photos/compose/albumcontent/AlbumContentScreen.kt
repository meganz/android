package mega.privacy.android.app.presentation.photos.compose.albumcontent

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.PhotoDownloaderViewModel
import mega.privacy.android.app.presentation.photos.albums.AlbumContentViewModel
import mega.privacy.android.app.presentation.photos.albums.AlbumsViewModel
import mega.privacy.android.app.presentation.photos.albums.model.getAlbumPhotos
import mega.privacy.android.app.presentation.photos.albums.view.CreateNewAlbumDialog
import mega.privacy.android.app.presentation.photos.albums.view.DeleteAlbumsConfirmationDialog
import mega.privacy.android.app.presentation.photos.albums.view.DynamicView
import mega.privacy.android.app.presentation.photos.albums.view.RemoveLinksConfirmationDialog
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.app.presentation.photos.view.FilterDialog
import mega.privacy.android.app.presentation.photos.view.RemovePhotosFromAlbumDialog
import mega.privacy.android.app.presentation.photos.view.SortByDialog
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.core.ui.controls.MegaEmptyView
import mega.privacy.android.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.core.ui.controls.progressindicator.MegaLinearProgressIndicator
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.dark_grey
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo

@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@Composable
fun AlbumContentScreen(
    photoDownloaderViewModel: PhotoDownloaderViewModel,
    timelineViewModel: TimelineViewModel,
    albumsViewModel: AlbumsViewModel,
    albumContentViewModel: AlbumContentViewModel,
    onNavigatePhotoPreview: (anchor: Photo, photos: List<Photo>) -> Unit,
    onNavigatePhotosSelection: (Album.UserAlbum) -> Unit,
) {
    val timelineState by timelineViewModel.state.collectAsStateWithLifecycle()
    val albumsState by albumsViewModel.state.collectAsStateWithLifecycle()
    val albumContentState by albumContentViewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val smallWidth = remember(configuration) {
        (configuration.screenWidthDp.dp - 1.dp) / 3
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        albumsState.currentUserAlbum?.also { album ->
            albumContentViewModel.observePhotosAddingProgress(albumId = album.id)
            albumContentViewModel.observePhotosRemovingProgress(albumId = album.id)
        }
    }

    if (albumContentState.isDeleteAlbum) {
        albumsViewModel.deleteAlbums(
            albumIds = listOfNotNull(albumsState.currentUserAlbum?.id),
        )

        albumsViewModel.updateAlbumDeletedMessage(
            message = pluralStringResource(
                id = R.plurals.photos_album_deleted_message,
                count = 1,
                albumsState.currentUserAlbum?.title.orEmpty(),
            ),
        )

        Back()
    }

    val photos = remember(
        albumsState.albums,
        albumsState.currentMediaType,
        albumsState.currentSort,
    ) {
        albumsState.currentAlbum?.let { album ->
            val sourcePhotos = albumsState.albums.getAlbumPhotos(album)
            if (sourcePhotos.isFilterable()) {
                sourcePhotos
                    .applyFilter(currentMediaType = albumsState.currentMediaType)
                    .applySortBy(currentSort = albumsState.currentSort)
            } else {
                albumsViewModel.setCurrentMediaType(FilterMediaType.DEFAULT)

                sourcePhotos
                    .applySortBy(currentSort = albumsState.currentSort)
            }
        }.orEmpty()
    }

    if (albumsState.showDeleteAlbumsConfirmation) {
        val album = albumsState.currentAlbum as? Album.UserAlbum
        DeleteAlbumsConfirmationDialog(
            selectedAlbumIds = listOfNotNull(album?.id),
            onCancelClicked = albumsViewModel::closeDeleteAlbumsConfirmation,
            onDeleteClicked = { albumContentViewModel.deleteAlbum() },
        )
    }

    if (albumContentState.showRemoveLinkConfirmation) {
        val album = albumsState.currentUserAlbum
        RemoveLinksConfirmationDialog(
            numLinks = 1,
            onCancel = albumContentViewModel::closeRemoveLinkConfirmation,
            onRemove = {
                albumContentViewModel.closeRemoveLinkConfirmation()
                album?.id?.let(albumContentViewModel::disableExportAlbum)
            },
        )
    }

    Box {
        if (photos.isNotEmpty()) {
            DynamicView(
                lazyListState = lazyListState,
                photos = photos,
                smallWidth = smallWidth,
                photoDownload = photoDownloaderViewModel::downloadPhoto,
                onClick = { photo ->
                    if (albumsState.selectedPhotos.isEmpty()) {
                        onNavigatePhotoPreview(photo, photos)
                    } else {
                        albumsViewModel.togglePhotoSelection(photo)
                    }
                },
                onLongPress = { photo ->
                    albumsViewModel.togglePhotoSelection(photo)
                },
                selectedPhotos = albumsState.selectedPhotos
            )

            if (albumContentState.isAddingPhotos || albumContentState.isRemovingPhotos) {
                MegaLinearProgressIndicator()
            }
        } else if (albumContentState.isAddingPhotos) {
            MegaCircularProgressIndicator(
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.Center)
            )
        } else {
            when (albumsState.currentAlbum) {
                Album.FavouriteAlbum -> MegaEmptyView(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_photos_favourite_album),
                    text = stringResource(id = R.string.empty_hint_favourite_album)
                        .formatColorTag(context, 'A', R.color.grey_900_grey_100)
                        .formatColorTag(context, 'B', R.color.grey_300_grey_600)
                        .toSpannedHtmlText()
                )

                Album.GifAlbum -> Back()
                Album.RawAlbum -> Back()
                is Album.UserAlbum -> MegaEmptyView(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_photos_user_album_empty),
                    text = stringResource(id = R.string.photos_user_album_empty_album)
                        .formatColorTag(context, 'A', R.color.grey_900_grey_100)
                        .formatColorTag(context, 'B', R.color.grey_300_grey_600)
                        .toSpannedHtmlText()
                )

                null -> Back()
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            val userAlbum = albumsState.currentUserAlbum
            if (userAlbum != null && albumContentState.isAddingPhotosProgressCompleted) {
                val message = pluralStringResource(
                    id = R.plurals.photos_album_selection_added,
                    count = albumContentState.totalAddedPhotos,
                    albumContentState.totalAddedPhotos,
                    userAlbum.title,
                )
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp),
                    backgroundColor = black.takeIf { MaterialTheme.colors.isLight } ?: white,
                ) {
                    Text(text = message)
                }

                LaunchedEffect(message) {
                    delay(3000L)
                    albumContentViewModel.updatePhotosAddingProgressCompleted(albumId = userAlbum.id)
                }
            }

            if (userAlbum != null && albumContentState.isRemovingPhotosProgressCompleted) {
                val message = pluralStringResource(
                    id = R.plurals.photos_album_photos_removal_snackbar_message,
                    count = albumContentState.totalRemovedPhotos,
                    albumContentState.totalRemovedPhotos,
                    userAlbum.title,
                )
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp),
                    backgroundColor = black.takeIf { MaterialTheme.colors.isLight } ?: white,
                ) {
                    Text(text = message)
                }

                LaunchedEffect(message) {
                    delay(3000L)
                    albumContentViewModel.updatePhotosRemovingProgressCompleted(albumId = userAlbum.id)
                }
            }

            if (userAlbum != null && albumContentState.isLinkRemoved) {
                val message = pluralStringResource(
                    id = R.plurals.context_link_removal_success,
                    count = 1,
                )
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp),
                    backgroundColor = black.takeIf { MaterialTheme.colors.isLight } ?: white,
                ) {
                    Text(text = message)
                }

                LaunchedEffect(message) {
                    delay(3000L)
                    albumContentViewModel.resetLinkRemoved()
                }
            }

            if (albumsState.snackBarMessage.isNotEmpty()) {
                SnackBar(
                    message = albumsState.snackBarMessage,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    onResetMessage = { albumsViewModel.setSnackBarMessage("") },
                )
            }

            val scrollNotInProgress by remember {
                derivedStateOf { !lazyListState.isScrollInProgress }
            }

            if (albumsState.currentMediaType != FilterMediaType.ALL_MEDIA && albumsState.selectedPhotos.isEmpty()) {
                AnimatedVisibility(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 24.dp),
                    visible = scrollNotInProgress,
                    exit = scaleOut(),
                    enter = scaleIn()
                ) {
                    FilterFabButton(
                        modifier = Modifier,
                        onFilterClick = { albumsViewModel.showFilterDialog(true) }
                    )
                }
                Spacer(modifier = Modifier.padding(top = 16.dp))
            }

            if (userAlbum != null && timelineState.photos.isNotEmpty() && albumsState.selectedPhotos.isEmpty() && !albumContentState.isAddingPhotos) {
                AnimatedVisibility(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    visible = scrollNotInProgress,
                    exit = scaleOut(),
                    enter = scaleIn()
                ) {
                    AddFabButton(
                        modifier = Modifier,
                        onNavigatePhotosSelection = { onNavigatePhotosSelection(userAlbum) }
                    )
                }
            }
        }

        if (albumsState.showSortByDialog) {
            SortByDialog(
                onDialogDismissed = {
                    albumsViewModel.showSortByDialog(showSortByDialog = false)
                },
                selectedOption = albumsState.currentSort,
                onOptionSelected = {
                    albumsViewModel.setCurrentSort(it)
                }
            )
        }

        if (albumsState.showFilterDialog) {
            FilterDialog(
                onDialogDismissed = {
                    albumsViewModel.showFilterDialog(showFilterDialog = false)
                },
                selectedOption = albumsState.currentMediaType,
                onOptionSelected = {
                    albumsViewModel.setCurrentMediaType(it)
                }
            )
        }

        if (albumsState.showRemovePhotosDialog) {
            RemovePhotosFromAlbumDialog(
                onDialogDismissed = {
                    with(albumsViewModel) {
                        clearSelectedPhotos()
                        setShowRemovePhotosFromAlbumDialog(false)
                    }
                },
                onPositiveButtonClick = {
                    with(albumsViewModel) {
                        removePhotosFromAlbum()
                        clearSelectedPhotos()
                        setShowRemovePhotosFromAlbumDialog(false)
                    }
                }
            )
        }

        if (albumsState.showRenameDialog) {
            CreateNewAlbumDialog(
                titleResID = R.string.context_rename,
                positiveButtonTextResID = R.string.context_rename,
                onDismissRequest = {
                    albumsViewModel.showRenameDialog(showRenameDialog = false)
                    albumsViewModel.setNewAlbumNameValidity(true)
                },
                onDialogPositiveButtonClicked = albumsViewModel::updateAlbumName,
                onDialogInputChange = albumsViewModel::setNewAlbumNameValidity,
                initialInputText = { (albumsState.currentAlbum as Album.UserAlbum).title },
                errorMessage = albumsState.createDialogErrorMessage,
            ) {
                albumsState.isInputNameValid
            }
        }

    }
}

@Composable
private fun AddFabButton(
    modifier: Modifier,
    onNavigatePhotosSelection: () -> Unit,
) {
    FloatingActionButton(
        onClick = onNavigatePhotosSelection,
        modifier = modifier
            .size(56.dp)
    ) {
        Icon(
            painter = painterResource(
                id = if (MaterialTheme.colors.isLight) {
                    R.drawable.ic_add_white
                } else {
                    R.drawable.ic_add
                }
            ),
            contentDescription = "Add",
            tint = if (!MaterialTheme.colors.isLight) {
                Color.Black
            } else {
                Color.White
            }
        )
    }
}

@Composable
private fun FilterFabButton(
    modifier: Modifier,
    onFilterClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onFilterClick,
        modifier = modifier
            .size(40.dp),
        backgroundColor = if (MaterialTheme.colors.isLight) {
            Color.White
        } else {
            dark_grey
        }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_filter_light),
            contentDescription = "Filter",
            tint = if (MaterialTheme.colors.isLight) {
                Color.Black
            } else {
                Color.White
            }
        )
    }
}

@Composable
private fun SnackBar(
    message: String,
    modifier: Modifier,
    onResetMessage: () -> Unit,
) {
    Snackbar(
        modifier = modifier.padding(8.dp),
        backgroundColor = black.takeIf { MaterialTheme.colors.isLight } ?: white,
    ) {
        Text(text = message)
    }

    LaunchedEffect(message) {
        delay(3000L)
        onResetMessage()
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

internal fun List<Photo>.isFilterable(): Boolean {
    val imageCount = this.count { it is Photo.Image }
    val videoCount = this.size - imageCount
    return imageCount > 0 && videoCount > 0
}

internal fun List<Photo>.applyFilter(currentMediaType: FilterMediaType) =
    when (currentMediaType) {
        FilterMediaType.ALL_MEDIA -> this
        FilterMediaType.IMAGES -> this.filterIsInstance<Photo.Image>()
        FilterMediaType.VIDEOS -> this.filterIsInstance<Photo.Video>()
    }

internal fun List<Photo>.applySortBy(currentSort: Sort) =
    if (currentSort == Sort.NEWEST) {
        this.sortedByDescending { it.modificationTime }
    } else {
        this.sortedBy { it.modificationTime }
    }
