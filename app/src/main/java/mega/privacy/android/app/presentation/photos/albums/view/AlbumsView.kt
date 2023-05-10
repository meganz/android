package mega.privacy.android.app.presentation.photos.albums.view

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import android.text.TextUtils
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.albums.model.AlbumTitle
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsViewState
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.app.utils.Util.dp2px
import mega.privacy.android.core.ui.controls.MegaDialog
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.button
import mega.privacy.android.core.ui.theme.caption
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.subtitle1
import mega.privacy.android.core.ui.theme.teal_200
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AlbumsView(
    albumsViewState: AlbumsViewState,
    openAlbum: (album: UIAlbum) -> Unit,
    downloadPhoto: PhotoDownload,
    onDialogPositiveButtonClicked: (name: String) -> Unit,
    setDialogInputPlaceholder: (String) -> Unit = {},
    setShowCreateAlbumDialog: (Boolean) -> Unit = {},
    setInputValidity: (Boolean) -> Unit = {},
    openPhotosSelectionActivity: (AlbumId) -> Unit = {},
    setIsAlbumCreatedSuccessfully: (Boolean) -> Unit = {},
    allPhotos: List<Photo> = emptyList(),
    clearAlbumDeletedMessage: () -> Unit = {},
    onAlbumSelection: (Album.UserAlbum) -> Unit = {},
    closeDeleteAlbumsConfirmation: () -> Unit = {},
    deleteAlbums: (albumIds: List<AlbumId>) -> Unit = {},
    lazyGridState: LazyGridState = LazyGridState(),
    onRemoveLinkDialogConfirmClick: () -> Unit = {},
    onRemoveLinkDialogCancelClick: () -> Unit = {},
    resetRemovedLinksCount: () -> Unit = {},
    isUserAlbumsEnabled: suspend () -> Boolean,
    isAlbumSharingEnabled: suspend () -> Boolean = { false },
) {
    val isLight = MaterialTheme.colors.isLight
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val grids = 3.takeIf { isPortrait } ?: 4

    val displayFAB by produceState(initialValue = false) {
        value = isUserAlbumsEnabled()
    }

    val displayLinkIcon by produceState(initialValue = false) {
        value = isAlbumSharingEnabled()
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

    if (albumsViewState.removedLinksCount > 0) {
        val snackbarMessage = pluralStringResource(
            id = R.plurals.context_link_removal_success,
            count = albumsViewState.removedLinksCount,
        )
        LaunchedEffect(Unit) {
            scaffoldState.snackbarHostState.showSnackbar(
                message = snackbarMessage,
            )
            delay(3000L)
            resetRemovedLinksCount()
        }
    }

    if (albumsViewState.showDeleteAlbumsConfirmation) {
        DeleteAlbumsConfirmationDialog(
            selectedAlbumIds = albumsViewState.selectedAlbumIds.toList(),
            onCancelClicked = closeDeleteAlbumsConfirmation,
            onDeleteClicked = deleteAlbums,
        )
    }

    if (albumsViewState.showRemoveAlbumLinkDialog) {
        RemoveLinksConfirmationDialog(
            numLinks = albumsViewState.selectedAlbumIds.size,
            onCancel = onRemoveLinkDialogCancelClick,
        ) {
            onRemoveLinkDialogConfirmClick()
        }
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
                            setShowCreateAlbumDialog(true)
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
                                        if (isAlbumSelected(
                                                album,
                                                albumsViewState.selectedAlbumIds
                                            )
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

                            AndroidView(
                                factory = {
                                    TextView(it).apply {
                                        maxLines = 1
                                        ellipsize = TextUtils.TruncateAt.MIDDLE
                                        textSize = 14f
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                            typeface = Typeface.create(null, 500, false)
                                        }

                                        setTextColor(
                                            ContextCompat.getColor(
                                                it,
                                                R.color.white.takeIf { !isLight } ?: R.color.black,
                                            )
                                        )
                                        setPadding(0, dp2px(10f), 0, dp2px(3f))
                                    }
                                },
                                update = { it.text = album.title.getTitleString(it.context) },
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

                        if (displayLinkIcon && isAlbumExported(album)) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_overlay),
                                contentScale = ContentScale.FillBounds,
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_link_white),
                                contentDescription = "${(album.title as AlbumTitle.StringTitle).title} Exported",
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(all = 8.dp),
                                tint = white,
                            )
                        }

                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
                content = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(44.dp),
                        color = teal_300,
                    )
                },
            )
        }
    }

    if (displayFAB && albumsViewState.selectedAlbumIds.isEmpty()) {
        if (albumsViewState.showCreateAlbumDialog) {
            CreateNewAlbumDialog(
                titleResID = R.string.photos_album_creation_dialog_title,
                positiveButtonTextResID = R.string.general_create,
                onDismissRequest = {
                    setShowCreateAlbumDialog(false)
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
        if (albumsViewState.isAlbumCreatedSuccessfully) {
            setIsAlbumCreatedSuccessfully(false)
            if (allPhotos.isNotEmpty()) {
                openPhotosSelectionActivity((albumsViewState.currentAlbum as Album.UserAlbum).id)
            }
        }
    }
}

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
        fontWeight = FontWeight.W500,
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

@Composable
fun RemoveLinksConfirmationDialog(
    numLinks: Int,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    MegaDialog(
        properties = DialogProperties(dismissOnClickOutside = false),
        onDismissRequest = onCancel,
        body = {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = pluralStringResource(
                    id = R.plurals.remove_links_warning_text,
                    count = numLinks,
                ),
                color = grey_alpha_054.takeIf { isLight } ?: white_alpha_054,
                style = subtitle1,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onRemove,
                content = {
                    Text(
                        text = stringResource(id = R.string.context_remove),
                        style = button,
                        color = teal_300.takeIf { isLight } ?: teal_200,
                    )
                },
            )
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                content = {
                    Text(
                        text = stringResource(id = R.string.button_cancel),
                        style = button,
                        color = teal_300.takeIf { isLight } ?: teal_200,
                    )
                },
            )
        },
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

private fun isAlbumExported(
    album: UIAlbum
): Boolean = album.id is Album.UserAlbum && album.id.isExported
