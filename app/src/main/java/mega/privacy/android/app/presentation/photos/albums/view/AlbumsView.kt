package mega.privacy.android.app.presentation.photos.albums.view

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsViewState
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.presentation.controls.MegaDialog
import mega.privacy.android.presentation.theme.black
import mega.privacy.android.presentation.theme.button
import mega.privacy.android.presentation.theme.caption
import mega.privacy.android.presentation.theme.grey_alpha_054
import mega.privacy.android.presentation.theme.subtitle1
import mega.privacy.android.presentation.theme.subtitle2
import mega.privacy.android.presentation.theme.teal_200
import mega.privacy.android.presentation.theme.teal_300
import mega.privacy.android.presentation.theme.white
import mega.privacy.android.presentation.theme.white_alpha_054

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AlbumsView(
    albumsViewState: AlbumsViewState,
    openAlbum: (album: UIAlbum) -> Unit,
    downloadPhoto: PhotoDownload,
    onDialogPositiveButtonClicked: (name: String, proscribedStrings: List<String>) -> Unit,
    setDialogInputPlaceholder: (String) -> Unit = {},
    setInputValidity: (Boolean) -> Unit = {},
    openPhotosSelectionActivity: (AlbumId) -> Unit = {},
    setIsAlbumCreatedSuccessfully: (Boolean) -> Unit = {},
    allPhotos: List<Photo> = emptyList(),
    clearAlbumDeletedMessage: () -> Unit = {},
    onAlbumSelection: (Album.UserAlbum) -> Unit = {},
    closeDeleteAlbumsConfirmation: () -> Unit = {},
    deleteAlbums: (albumIds: List<AlbumId>) -> Unit = {},
    lazyGridState: LazyGridState = LazyGridState(),
    isUserAlbumsEnabled: suspend () -> Boolean,
) {
    val isLight = MaterialTheme.colors.isLight
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val grids = 3.takeIf { isPortrait } ?: 4
    val openDialog = rememberSaveable { mutableStateOf(false) }

    val displayFAB by produceState(initialValue = false) {
        value = isUserAlbumsEnabled()
    }
    val scaffoldState = rememberScaffoldState()

    if (albumsViewState.albumDeletedMessage.isNotEmpty()) {
        LaunchedEffect(Unit) {
            scaffoldState.snackbarHostState.showSnackbar(
                message = albumsViewState.albumDeletedMessage,
            )
            delay(3000L)
            clearAlbumDeletedMessage()
        }
    }

    if (albumsViewState.showDeleteAlbumsConfirmation) {
        DeleteAlbumsConfirmationDialog(
            selectedAlbumIds = albumsViewState.selectedAlbumIds.toList(),
            onCancelClicked = closeDeleteAlbumsConfirmation,
            onDeleteClicked = deleteAlbums,
        )
    }

    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { snackbarHostState ->
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        backgroundColor = black.takeIf { isLight } ?: white,
                    )
                }
            )
        },
        floatingActionButton = {
            if (displayFAB && albumsViewState.selectedAlbumIds.isEmpty()) {
                val placeholderText =
                    stringResource(id = R.string.photos_album_creation_dialog_input_placeholder)
                val scrollNotInProgress by remember {
                    derivedStateOf { !lazyGridState.isScrollInProgress }
                }
                AnimatedVisibility(
                    visible = scrollNotInProgress,
                    exit = scaleOut(),
                    enter = scaleIn()
                ) {
                    FloatingActionButton(
                        onClick = {
                            openDialog.value = true
                            setDialogInputPlaceholder(placeholderText)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Create new album",
                            tint = if (!MaterialTheme.colors.isLight) {
                                Color.Black
                            } else {
                                Color.White
                            }
                        )
                    }
                }
            }
        },
    ) {
        //We need to wait system album load fist and then show the list of album
        if (albumsViewState.showAlbums) {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                columns = GridCells.Fixed(grids),
                state = lazyGridState,
            ) {
                items(
                    items = albumsViewState.albums,
                    key = { it.id.toString() + it.coverPhoto?.id.toString() }
                ) { album ->
                    Box(
                        modifier = Modifier
                            .alpha(1f.takeIf {
                                album.id is Album.UserAlbum || albumsViewState.selectedAlbumIds.isEmpty()
                            } ?: 0.5f)
                            .combinedClickable(
                                onClick = {
                                    handleAlbumClicked(
                                        album = album,
                                        numSelectedAlbums = albumsViewState.selectedAlbumIds.size,
                                        openAlbum = openAlbum,
                                        onAlbumSelection = onAlbumSelection,
                                    )
                                },
                                onLongClick = {
                                    handleAlbumLongPressed(
                                        album = album,
                                        onAlbumSelection = onAlbumSelection,
                                    )
                                }
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .fillMaxSize()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val imageState = produceState<String?>(initialValue = null) {
                                album.coverPhoto?.let {
                                    downloadPhoto(
                                        false,
                                        it
                                    ) { downloadSuccess ->
                                        if (downloadSuccess) {
                                            value = it.thumbnailFilePath
                                        }
                                    }
                                }
                            }
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageState.value)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                placeholder = if (!MaterialTheme.colors.isLight) {
                                    painterResource(id = R.drawable.ic_album_cover_d)
                                } else {
                                    painterResource(id = R.drawable.ic_album_cover)
                                },
                                error = if (!MaterialTheme.colors.isLight) {
                                    painterResource(id = R.drawable.ic_album_cover_d)
                                } else {
                                    painterResource(id = R.drawable.ic_album_cover)
                                },
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .aspectRatio(1f)
                                    .then(
                                        if (isAlbumSelected(album,
                                                albumsViewState.selectedAlbumIds)
                                        )
                                            Modifier.border(
                                                BorderStroke(
                                                    width = 1.dp,
                                                    color = colorResource(id = R.color.teal_300),
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                            )
                                        else Modifier
                                    ),
                            )
                            MiddleEllipsisText(
                                modifier = Modifier.padding(top = 10.dp, bottom = 3.dp),
                                text = album.title,
                                style = subtitle2,
                                color = if (MaterialTheme.colors.isLight) black else white,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = album.count.toString(),
                                style = caption,
                                color = if (MaterialTheme.colors.isLight) grey_alpha_054 else white_alpha_054,
                            )
                        }
                        if (isAlbumSelected(album, albumsViewState.selectedAlbumIds)) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_select_folder),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp),
                                tint = Color.Unspecified,
                            )
                        }
                    }
                }
            }
        }
    }

    if (displayFAB && albumsViewState.selectedAlbumIds.isEmpty()) {
        if (!albumsViewState.isAlbumCreatedSuccessfully) {
            if (openDialog.value) {
                CreateNewAlbumDialog(
                    onDismissRequest = {
                        openDialog.value = false
                        setInputValidity(true)
                    },
                    onDialogPositiveButtonClicked = onDialogPositiveButtonClicked,
                    onDialogInputChange = setInputValidity,
                    inputPlaceHolderText = { albumsViewState.createAlbumPlaceholderTitle },
                    errorMessage = albumsViewState.createDialogErrorMessage,
                ) {
                    albumsViewState.isInputNameValid
                }
            }
        } else {
            setIsAlbumCreatedSuccessfully(false)
            openDialog.value = false
            if (allPhotos.isNotEmpty()) {
                openPhotosSelectionActivity((albumsViewState.currentAlbum as Album.UserAlbum).id)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DeleteAlbumsConfirmationDialog(
    selectedAlbumIds: List<AlbumId>,
    onCancelClicked: () -> Unit,
    onDeleteClicked: (albumIds: List<AlbumId>) -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    MegaDialog(
        titleString = pluralStringResource(
            id = R.plurals.photos_album_delete_confirmation_title,
            count = selectedAlbumIds.size,
        ),
        body = {
            Text(
                modifier = Modifier.padding(),
                text = pluralStringResource(
                    id = R.plurals.photos_album_delete_confirmation_description,
                    count = selectedAlbumIds.size,
                ),
                color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                style = subtitle1,
            )
        },
        onDismissRequest = onCancelClicked,
        confirmButton = {
            TextButton(
                onClick = {
                    onCancelClicked()
                    onDeleteClicked(selectedAlbumIds)
                },
            ) {
                Text(
                    text = stringResource(id = R.string.delete_button),
                    style = button,
                    color = teal_300.takeIf { isLight } ?: teal_200
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onCancelClicked()
                },
            ) {
                Text(
                    text = stringResource(id = R.string.button_cancel),
                    style = button,
                    color = teal_300.takeIf { isLight } ?: teal_200
                )
            }
        },
        properties = DialogProperties(dismissOnClickOutside = false),
    )
}

private fun handleAlbumClicked(
    album: UIAlbum,
    numSelectedAlbums: Int,
    openAlbum: (UIAlbum) -> Unit,
    onAlbumSelection: (Album.UserAlbum) -> Unit,
) {
    if (numSelectedAlbums == 0) {
        openAlbum(album)
    } else if (album.id is Album.UserAlbum) {
        onAlbumSelection(album.id)
    }
}

private fun handleAlbumLongPressed(
    album: UIAlbum,
    onAlbumSelection: (Album.UserAlbum) -> Unit,
) {
    if (album.id is Album.UserAlbum) {
        onAlbumSelection(album.id)
    }
}

private fun isAlbumSelected(
    album: UIAlbum,
    selectedAlbumIds: Set<AlbumId>,
): Boolean = album.id is Album.UserAlbum && album.id.id in selectedAlbumIds
