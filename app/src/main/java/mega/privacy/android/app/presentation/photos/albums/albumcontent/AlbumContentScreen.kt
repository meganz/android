package mega.privacy.android.app.presentation.photos.albums.albumcontent

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.PhotoDownloaderViewModel
import mega.privacy.android.app.presentation.photos.albums.AlbumsViewModel
import mega.privacy.android.app.presentation.photos.albums.view.CreateNewAlbumDialog
import mega.privacy.android.app.presentation.photos.albums.view.DeleteAlbumsConfirmationDialog
import mega.privacy.android.app.presentation.photos.albums.view.DynamicView
import mega.privacy.android.app.presentation.photos.albums.view.RemoveLinksConfirmationDialog
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.timeline.view.AlbumContentSkeletonView
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.app.presentation.photos.view.FilterDialog
import mega.privacy.android.app.presentation.photos.view.RemovePhotosFromAlbumDialog
import mega.privacy.android.app.presentation.photos.view.SortByDialog
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.core.ui.controls.progressindicator.MegaLinearProgressIndicator
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.dark_grey
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.domain.entity.photos.Album.FavouriteAlbum
import mega.privacy.android.domain.entity.photos.Album.GifAlbum
import mega.privacy.android.domain.entity.photos.Album.RawAlbum
import mega.privacy.android.domain.entity.photos.Album.UserAlbum
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView
import mega.privacy.mobile.analytics.event.AlbumAddPhotosFABEvent
import mega.privacy.mobile.analytics.event.RemoveItemsFromAlbumDialogButtonEvent

@Composable
internal fun AlbumContentScreen(
    photoDownloaderViewModel: PhotoDownloaderViewModel,
    timelineViewModel: TimelineViewModel,
    albumsViewModel: AlbumsViewModel,
    albumContentViewModel: AlbumContentViewModel,
    onNavigatePhotoPreview: (anchor: Photo, photos: List<Photo>) -> Unit,
    onNavigatePhotosSelection: (UserAlbum) -> Unit,
) {
    val timelineState by timelineViewModel.state.collectAsStateWithLifecycle()
    val albumContentState by albumContentViewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val smallWidth = remember(configuration) {
        (configuration.screenWidthDp.dp - 1.dp) / 3
    }

    val lazyListState = rememberLazyListState()

    if (albumContentState.isDeleteAlbum) {
        val album = albumContentState.uiAlbum?.id as? UserAlbum

        albumsViewModel.deleteAlbums(
            albumIds = listOfNotNull(album?.id),
        )

        albumsViewModel.updateAlbumDeletedMessage(
            message = stringResource(
                id = R.string.photos_album_deleted_message_singular,
                albumContentState.uiAlbum?.title?.getTitleString(context).orEmpty(),
            ),
        )

        Back()
    }

    val photos = remember(
        albumContentState.photos,
        albumContentState.currentMediaType,
        albumContentState.currentSort,
    ) {
        albumContentState.photos.let { photos ->
            if (photos.isFilterable()) {
                photos
                    .applyFilter(currentMediaType = albumContentState.currentMediaType)
                    .applySortBy(currentSort = albumContentState.currentSort)
            } else {
                albumContentViewModel.setCurrentMediaType(FilterMediaType.DEFAULT)
                photos
                    .applySortBy(currentSort = albumContentState.currentSort)
            }
        }
    }

    if (albumContentState.showDeleteAlbumsConfirmation) {
        val album = albumContentState.uiAlbum?.id as? UserAlbum
        DeleteAlbumsConfirmationDialog(
            selectedAlbumIds = listOfNotNull(album?.id),
            onCancelClicked = albumContentViewModel::closeDeleteAlbumsConfirmation,
            onDeleteClicked = { albumContentViewModel.deleteAlbum() },
        )
    }

    if (albumContentState.showRemoveLinkConfirmation) {
        val album = albumContentState.uiAlbum?.id as? UserAlbum
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
        if (photos.isEmpty() && (albumContentState.isLoading || albumContentState.isAddingPhotos)) {
            AlbumContentSkeletonView(smallWidth = smallWidth)
        } else if (!albumContentState.isLoading && albumContentState.uiAlbum == null) {
            Back()
        } else if (photos.isNotEmpty()) {
            DynamicView(
                lazyListState = lazyListState,
                photos = photos,
                smallWidth = smallWidth,
                photoDownload = photoDownloaderViewModel::downloadPhoto,
                onClick = { photo ->
                    if (albumContentState.selectedPhotos.isEmpty()) {
                        onNavigatePhotoPreview(photo, photos)
                    } else {
                        albumContentViewModel.togglePhotoSelection(photo)
                    }
                },
                onLongPress = { photo ->
                    albumContentViewModel.togglePhotoSelection(photo)
                },
                selectedPhotos = albumContentState.selectedPhotos
            )

            if (albumContentState.isAddingPhotos || albumContentState.isRemovingPhotos) {
                MegaLinearProgressIndicator(progress = null)
            }
        } else {
            when (albumContentState.uiAlbum?.id) {
                FavouriteAlbum -> LegacyMegaEmptyView(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_photos_favourite_album),
                    text = stringResource(id = R.string.empty_hint_favourite_album)
                        .formatColorTag(context, 'A', R.color.grey_900_grey_100)
                        .formatColorTag(context, 'B', R.color.grey_300_grey_600)
                        .toSpannedHtmlText()
                )

                GifAlbum -> Back()

                RawAlbum -> Back()

                is UserAlbum -> LegacyMegaEmptyView(
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
            val userAlbum = albumContentState.uiAlbum?.id as? UserAlbum
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

            if (albumContentState.snackBarMessage.isNotEmpty()) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp),
                    backgroundColor = black.takeIf { MaterialTheme.colors.isLight } ?: white,
                ) {
                    Text(text = albumContentState.snackBarMessage)
                }

                LaunchedEffect(albumContentState.snackBarMessage) {
                    delay(3000L)
                    albumContentViewModel.setSnackBarMessage("")
                }
            }

            val scrollNotInProgress by remember {
                derivedStateOf { !lazyListState.isScrollInProgress }
            }

            if (albumContentState.currentMediaType != FilterMediaType.ALL_MEDIA && albumContentState.selectedPhotos.isEmpty()) {
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
                        onFilterClick = { albumContentViewModel.showFilterDialog(true) }
                    )
                }
                Spacer(modifier = Modifier.padding(top = 16.dp))
            }

            if (userAlbum != null && timelineState.photos.isNotEmpty() && albumContentState.selectedPhotos.isEmpty() && !albumContentState.isAddingPhotos) {
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
                        onNavigatePhotosSelection = {
                            Analytics.tracker.trackEvent(AlbumAddPhotosFABEvent)
                            onNavigatePhotosSelection(userAlbum)
                        }
                    )
                }
            }
        }

        if (albumContentState.showSortByDialog) {
            SortByDialog(
                onDialogDismissed = {
                    albumContentViewModel.showSortByDialog(showSortByDialog = false)
                },
                selectedOption = albumContentState.currentSort,
                onOptionSelected = albumContentViewModel::setCurrentSort,
            )
        }

        if (albumContentState.showFilterDialog) {
            FilterDialog(
                onDialogDismissed = {
                    albumContentViewModel.showFilterDialog(showFilterDialog = false)
                },
                selectedOption = albumContentState.currentMediaType,
                onOptionSelected = {
                    albumContentViewModel.setCurrentMediaType(it)
                }
            )
        }

        if (albumContentState.showRemovePhotosDialog) {
            RemovePhotosFromAlbumDialog(
                onDialogDismissed = {
                    with(albumContentViewModel) {
                        clearSelectedPhotos()
                        setShowRemovePhotosFromAlbumDialog(false)
                    }
                },
                onPositiveButtonClick = {
                    Analytics.tracker.trackEvent(RemoveItemsFromAlbumDialogButtonEvent)
                    with(albumContentViewModel) {
                        removePhotosFromAlbum()
                        clearSelectedPhotos()
                        setShowRemovePhotosFromAlbumDialog(false)
                    }
                }
            )
        }

        if (albumContentState.showRenameDialog) {
            val albumName = albumContentState.uiAlbum?.title?.getTitleString(context).orEmpty()

            CreateNewAlbumDialog(
                titleResID = R.string.context_rename,
                positiveButtonTextResID = R.string.context_rename,
                onDismissRequest = {
                    albumContentViewModel.showRenameDialog(false)
                    albumContentViewModel.setNewAlbumNameValidity(true)
                },
                onDialogPositiveButtonClicked = {
                    albumContentViewModel.updateAlbumName(
                        title = it,
                        albumNames = albumsViewModel.getAllUserAlbumsNames(),
                    )
                },
                onDialogInputChange = albumContentViewModel::setNewAlbumNameValidity,
                initialInputText = { albumName },
                errorMessage = albumContentState.createDialogErrorMessage,
            ) {
                albumContentState.isInputNameValid
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
private fun Back() {
    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    LaunchedEffect(key1 = true) {
        onBackPressedDispatcher?.onBackPressed()
    }
}

private fun List<Photo>.isFilterable(): Boolean {
    val imageCount = this.count { it is Photo.Image }
    val videoCount = this.size - imageCount
    return imageCount > 0 && videoCount > 0
}

private fun List<Photo>.applyFilter(currentMediaType: FilterMediaType) =
    when (currentMediaType) {
        FilterMediaType.ALL_MEDIA -> this
        FilterMediaType.IMAGES -> this.filterIsInstance<Photo.Image>()
        FilterMediaType.VIDEOS -> this.filterIsInstance<Photo.Video>()
    }

private fun List<Photo>.applySortBy(currentSort: Sort) =
    if (currentSort == Sort.NEWEST) {
        this.sortedWith(compareByDescending<Photo> { it.modificationTime }.thenByDescending { it.id })
    } else {
        this.sortedWith(compareBy<Photo> { it.modificationTime }.thenByDescending { it.id })
    }
