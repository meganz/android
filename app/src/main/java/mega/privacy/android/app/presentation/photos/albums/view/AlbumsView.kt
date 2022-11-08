package mega.privacy.android.app.presentation.photos.albums.view

import androidx.compose.foundation.background
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsViewState
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.presentation.theme.black
import mega.privacy.android.presentation.theme.grey_alpha_054
import mega.privacy.android.presentation.theme.teal_300
import mega.privacy.android.presentation.theme.white
import mega.privacy.android.presentation.theme.white_alpha_054

@Composable
fun AlbumsView(
    albumsViewState: AlbumsViewState,
    openAlbum: (album: UIAlbum) -> Unit,
    downloadPhoto: PhotoDownload,
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
                    Text(
                        modifier = Modifier.padding(top = 10.dp, bottom = 3.dp),
                        text = album.title(LocalContext.current),
                        style = MaterialTheme.typography.subtitle2,
                        color = if (MaterialTheme.colors.isLight) black else white,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = album.count.toString(),
                        style = MaterialTheme.typography.caption,
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
            FloatingActionButton(
                modifier = Modifier.padding(all = 16.dp),
                onClick = { openDialog.value = true }
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

        if (openDialog.value) {
            CreateNewAlbumDialog(
                onDismissRequest = { openDialog.value = false }
            )
        }
    }

}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CreateNewAlbumDialog(
    onDismissRequest: () -> Unit = {},
) {
    var textState by rememberSaveable { mutableStateOf("") }
    val isEnabled by remember { mutableStateOf(true) }
    val isError by remember { mutableStateOf(false) }

    val singleLine = true
    MaterialTheme {
        Dialog(onDismissRequest = onDismissRequest) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colors.surface,
            ) {
                Column {
                    // Dialog title
                    Text(
                        text = stringResource(id = R.string.photos_album_creation_dialog_title),
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp),
                    )

                    val textFieldColors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent,
                        cursorColor = teal_300,
                        focusedIndicatorColor = teal_300,
                        unfocusedIndicatorColor = teal_300,
                    )

                    val interactionSource = remember { MutableInteractionSource() }

                    val textColor = LocalTextStyle.current.color.takeOrElse {
                        textFieldColors.textColor(isEnabled).value
                    }
                    val mergedTextStyle = LocalTextStyle.current.merge(TextStyle(color = textColor))

                    BasicTextField(
                        value = textState,
                        onValueChange = {
                            textState = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 16.dp)
                            .background(color = Color.Transparent)
                            .indicatorLine(
                                enabled = isEnabled,
                                isError = isError,
                                interactionSource = interactionSource,
                                colors = textFieldColors
                            ),
                        cursorBrush = SolidColor(textFieldColors.cursorColor(isError).value),
                        textStyle = mergedTextStyle,
                        maxLines = 1,
                        singleLine = singleLine,
                        decorationBox = @Composable { innerTextField ->
                            // places leading icon, text field with label and placeholder, trailing icon
                            TextFieldDefaults.TextFieldDecorationBox(
                                enabled = isEnabled,
                                interactionSource = interactionSource,
                                singleLine = singleLine,
                                visualTransformation = VisualTransformation.None,
                                value = textState,
                                innerTextField = innerTextField,
                                placeholder = {
                                    Text(text = stringResource(
                                        id = R.string.photos_album_creation_dialog_input_placeholder
                                    ))
                                },
                                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp),
                                colors = textFieldColors,
                            )
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { onDismissRequest() },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Transparent,
                            ),
                            modifier = Modifier.padding(all = 0.dp),
                            elevation = ButtonDefaults.elevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                disabledElevation = 0.dp,
                                hoveredElevation = 0.dp,
                                focusedElevation = 0.dp
                            ),
                        ) {
                            Text(
                                stringResource(id = R.string.general_cancel),
                                color = teal_300
                            )
                        }
                        Button(
                            onClick = { onDismissRequest() },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Transparent,
                            ),
                            modifier = Modifier.padding(all = 0.dp),
                            elevation = ButtonDefaults.elevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                disabledElevation = 0.dp,
                                hoveredElevation = 0.dp,
                                focusedElevation = 0.dp
                            ),
                        ) {
                            Text(
                                text = stringResource(id = R.string.general_create),
                                color = teal_300
                            )
                        }
                    }
                }
            }
        }
    }
}

