package mega.privacy.android.app.presentation.photos.albums.view

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsViewState
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.presentation.theme.black
import mega.privacy.android.presentation.theme.caption
import mega.privacy.android.presentation.theme.grey_alpha_054
import mega.privacy.android.presentation.theme.subtitle2
import mega.privacy.android.presentation.theme.white
import mega.privacy.android.presentation.theme.white_alpha_054

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
    isUserAlbumsEnabled: suspend () -> Boolean,
) {
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val grids = 3.takeIf { isPortrait } ?: 4
    val openDialog = rememberSaveable { mutableStateOf(false) }

    val displayFAB by produceState(initialValue = false) {
        value = isUserAlbumsEnabled()
    }

    LazyVerticalGrid(
        contentPadding = PaddingValues(top = 8.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        columns = GridCells.Fixed(grids),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = albumsViewState.albums,
            key = { it.id.toString() + it.coverPhoto?.id.toString() }
        ) { album ->
            Box(
                modifier = Modifier
                    .clickable {
                        openAlbum(album)
                    }
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
            }
        }
    }

    if (displayFAB) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd,
        ) {
            val placeholderText =
                stringResource(id = R.string.photos_album_creation_dialog_input_placeholder)
            FloatingActionButton(
                modifier = Modifier.padding(all = 16.dp),
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
