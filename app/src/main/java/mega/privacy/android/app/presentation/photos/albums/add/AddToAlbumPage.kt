package mega.privacy.android.app.presentation.photos.albums.add

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.albums.view.CreateNewAlbumDialog
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.app.presentation.photos.timeline.view.AlbumListSkeletonView
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.photos.Album.UserAlbum
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.extensions.body3
import mega.privacy.android.shared.original.core.ui.theme.extensions.body4
import mega.android.core.ui.theme.values.TextColor

@Composable
internal fun AddToAlbumPage(
    modifier: Modifier = Modifier,
    accountType: AccountType?,
    isBusinessAccountExpired: Boolean,
    isLoading: Boolean,
    albums: List<Pair<UserAlbum, Int>>,
    selectedAlbum: UserAlbum?,
    isCreatingAlbum: Boolean,
    albumNameSuggestion: String,
    albumNameErrorMessageRes: Int?,
    onDownloadPhoto: PhotoDownload,
    onSelectAlbum: (UserAlbum) -> Unit,
    onSetupNewAlbum: (String) -> Unit,
    onCancelAlbumCreation: () -> Unit,
    onClearAlbumNameErrorMessage: () -> Unit,
    onCreateAlbum: (String) -> Unit,
) {
    val defaultAlbumName = stringResource(R.string.photos_album_creation_dialog_input_placeholder)

    if (isCreatingAlbum) {
        CreateNewAlbumDialog(
            titleResID = R.string.photos_album_creation_dialog_title,
            positiveButtonTextResID = R.string.general_create,
            onDismissRequest = {
                onCancelAlbumCreation()
                onClearAlbumNameErrorMessage()
            },
            onDialogPositiveButtonClicked = {
                val albumName = it.trim().ifEmpty { albumNameSuggestion }
                onCreateAlbum(albumName)
            },
            onDialogInputChange = { onClearAlbumNameErrorMessage() },
            inputPlaceHolderText = { albumNameSuggestion },
            errorMessage = albumNameErrorMessageRes,
            isInputValid = { albumNameErrorMessageRes == null },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface),
        content = {
            TextMegaButton(
                text = stringResource(id = R.string.photos_album_creation_dialog_input_placeholder),
                onClick = { onSetupNewAlbum(defaultAlbumName) },
                contentPadding = PaddingValues(horizontal = 16.dp)
            )

            if (isLoading) {
                AlbumListSkeletonView()
            } else if (albums.isEmpty()) {
                EmptyState()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    content = {
                        items(
                            items = albums,
                            key = { (album, count) ->
                                "${album.id}-${album.cover}-${count}"
                            },
                            itemContent = { (album, count) ->
                                AlbumCard(
                                    accountType = accountType,
                                    isBusinessAccountExpired = isBusinessAccountExpired,
                                    album = album,
                                    count = count,
                                    isSelected = album.id == selectedAlbum?.id,
                                    onDownloadPhoto = onDownloadPhoto,
                                    onClick = onSelectAlbum,
                                )
                            },
                        )
                    },
                )
            }
        },
    )
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Image(
                painter = painterResource(id = iconPackR.drawable.ic_playlist_glass_red),
                contentDescription = null,
            )

            Spacer(modifier = Modifier.height(16.dp))

            MegaText(
                text = "No Albums",
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.body1,
            )
        },
    )
}

@Composable
private fun AlbumCard(
    modifier: Modifier = Modifier,
    accountType: AccountType?,
    isBusinessAccountExpired: Boolean,
    album: UserAlbum,
    count: Int,
    isSelected: Boolean,
    onDownloadPhoto: PhotoDownload,
    onClick: (UserAlbum) -> Unit,
) {
    val context = LocalContext.current
    val isLight = MaterialTheme.colors.isLight

    val coverData by produceState<String?>(
        initialValue = null,
        key1 = album,
    ) {
        album.cover?.let { photo ->
            onDownloadPhoto(false, photo) { isSuccess ->
                if (isSuccess) {
                    value = photo.thumbnailFilePath
                }
            }
        }
    }

    val isCoverSensitive = accountType?.isPaid == true
            && !isBusinessAccountExpired
            && (album.cover?.isSensitive == true || album.cover?.isSensitiveInherited == true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable { onClick(album) },
        content = {
            Column(
                content = {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(coverData)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .aspectRatio(1f)
                            .then(
                                if (isSelected)
                                    Modifier.border(
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = colorResource(id = R.color.accent_900),
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                else Modifier
                            )
                            .alpha(0.5f.takeIf { isCoverSensitive } ?: 1f)
                            .blur(16.dp.takeIf { isCoverSensitive } ?: 0.dp),
                        placeholder = painterResource(
                            id = R.drawable.ic_album_cover_d.takeIf { !isLight }
                                ?: R.drawable.ic_album_cover,
                        ),
                        error = painterResource(
                            id = R.drawable.ic_album_cover_d.takeIf { !isLight }
                                ?: R.drawable.ic_album_cover,
                        ),
                        contentScale = ContentScale.FillWidth,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    MegaText(
                        text = album.title,
                        textColor = TextColor.Primary,
                        overflow = LongTextBehaviour.MiddleEllipsis,
                        style = MaterialTheme.typography.body3,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    MegaText(
                        text = "$count",
                        textColor = TextColor.Secondary,
                        overflow = LongTextBehaviour.MiddleEllipsis,
                        style = MaterialTheme.typography.body4,
                    )
                },
            )

            RadioButton(
                selected = isSelected,
                onClick = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                colors = RadioButtonDefaults.colors(
                    unselectedColor = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                ),
            )
        },
    )
}
