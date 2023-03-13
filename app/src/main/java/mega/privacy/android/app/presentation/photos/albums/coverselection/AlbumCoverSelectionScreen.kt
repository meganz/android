package mega.privacy.android.app.presentation.photos.albums.coverselection

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.UIPhoto
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.presentation.photos.view.PhotosGridView
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.dark_grey
import mega.privacy.android.core.ui.theme.teal_200
import mega.privacy.android.core.ui.theme.teal_200_alpha_038
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.teal_300_alpha_038
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.domain.entity.photos.Photo

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AlbumCoverSelectionScreen(
    viewModel: AlbumCoverSelectionViewModel = viewModel(),
    onBackClicked: () -> Unit,
    onCompletion: (message: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lazyGridState = rememberLazyGridState()

    if (state.isInvalidAlbum) onBackClicked()

    HandleAlbumCoverCompletion(
        isSelectionCompleted = state.isSelectionCompleted,
        onCompletion = onCompletion,
    )

    Scaffold(
        topBar = {
            AlbumCoverSelectionHeader(
                onBackClicked = onBackClicked,
            )
        },
        content = {
            Box {
                AlbumCoverSelectionContent(
                    lazyGridState = lazyGridState,
                    uiPhotos = state.uiPhotos,
                    selectedPhoto = state.selectedPhoto,
                    onPhotoDownload = viewModel::downloadPhoto,
                    onPhotoSelection = viewModel::selectPhoto,
                )

                AlbumCoverSelectionFooter(
                    modifier = Modifier
                        .align(Alignment.BottomCenter),
                    hasSelectedPhoto = state.hasSelectedPhoto,
                    onBackClicked = onBackClicked,
                    onUpdateCover = {
                        viewModel.updateCover(
                            album = state.album,
                            photo = state.selectedPhoto,
                        )
                    },
                )
            }
        },
    )
}

@Composable
private fun AlbumCoverSelectionHeader(
    onBackClicked: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.photos_album_select_cover),
                fontWeight = FontWeight.W500,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.subtitle1,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back_white),
                    contentDescription = null,
                    tint = black.takeIf { isLight } ?: white,
                )
            }
        },
        elevation = 0.dp,
    )
}

@Composable
private fun AlbumCoverSelectionContent(
    lazyGridState: LazyGridState,
    uiPhotos: List<UIPhoto>,
    selectedPhoto: Photo?,
    onPhotoDownload: PhotoDownload,
    onPhotoSelection: (Photo) -> Unit,
) {
    PhotosGridView(
        currentZoomLevel = ZoomLevel.Grid_3,
        endSpacing = 88.dp,
        photoDownland = { _, photo, callback ->
            onPhotoDownload(photo, callback)
        },
        lazyGridState = lazyGridState,
        onClick = onPhotoSelection,
        onLongPress = onPhotoSelection,
        selectedPhotoIds = setOfNotNull(selectedPhoto?.id),
        uiPhotoList = uiPhotos,
    )
}

@Composable
private fun AlbumCoverSelectionFooter(
    modifier: Modifier,
    hasSelectedPhoto: Boolean,
    onBackClicked: () -> Unit,
    onUpdateCover: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(white.takeIf { isLight } ?: dark_grey)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.button_cancel),
            modifier = Modifier
                .clickable { onBackClicked() },
            color = teal_200,
            fontWeight = FontWeight.W500,
            style = MaterialTheme.typography.button,
        )

        Spacer(modifier = Modifier.size(28.dp))

        Button(
            onClick = onUpdateCover,
            enabled = hasSelectedPhoto,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = teal_300.takeIf { isLight } ?: teal_200,
                disabledBackgroundColor = teal_300_alpha_038.takeIf { isLight }
                    ?: teal_200_alpha_038,
            ),
        ) {
            Text(
                text = stringResource(id = R.string.save_action),
                color = white.takeIf { isLight } ?: dark_grey,
                fontWeight = FontWeight.W500,
                style = MaterialTheme.typography.button,
            )
        }
    }
}

@Composable
private fun HandleAlbumCoverCompletion(
    isSelectionCompleted: Boolean,
    onCompletion: (message: String) -> Unit,
) {
    if (isSelectionCompleted) {
        val message = stringResource(id = R.string.photos_album_cover_updated)
        onCompletion(message)
    }
}
