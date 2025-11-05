package mega.privacy.android.feature.photos.presentation.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.components.AlbumGridItem
import mega.privacy.android.feature.photos.extensions.downloadAsStateWithLifecycle
import mega.privacy.android.feature.photos.navigation.AlbumContentNavKey
import mega.privacy.android.feature.photos.presentation.albums.content.toAlbumContentNavKey
import mega.privacy.android.feature.photos.presentation.albums.dialog.AddNewAlbumDialog

@Composable
fun AlbumsTabRoute(
    navigateToAlbumContent: (AlbumContentNavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumsTabViewModel = hiltViewModel(),
    showNewAlbumDialogEvent: StateEvent = consumed,
    resetNewAlbumDialogEvent: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AlbumsTabScreen(
        uiState = uiState,
        addNewAlbum = viewModel::addNewAlbum,
        navigateToAlbumContent = navigateToAlbumContent,
        modifier = modifier,
        showNewAlbumDialogEvent = showNewAlbumDialogEvent,
        resetNewAlbumDialogEvent = resetNewAlbumDialogEvent
    )
}

@Composable
fun AlbumsTabScreen(
    uiState: AlbumsTabUiState,
    addNewAlbum: (String) -> Unit,
    navigateToAlbumContent: (AlbumContentNavKey) -> Unit,
    modifier: Modifier = Modifier,
    showNewAlbumDialogEvent: StateEvent = consumed,
    resetNewAlbumDialogEvent: () -> Unit = {},
) {
    val placeholder = if (isSystemInDarkTheme()) {
        painterResource(R.drawable.ic_album_cover_d)
    } else {
        painterResource(R.drawable.ic_album_cover)
    }

    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Adaptive(120.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(
            count = uiState.albums.size,
            key = { uiState.albums[it].mediaAlbum.hashCode() },
            contentType = { index ->
                uiState.albums[index]::class
            }
        ) { index ->
            val album = uiState.albums[index]
            val downloadResult = album.cover?.downloadAsStateWithLifecycle(isPreview = false)

            AlbumGridItem(
                modifier = Modifier
                    .testTag("$ALBUMS_SCREEN_ALBUM_GRID_ITEM:${index}")
                    .clickable {
                        navigateToAlbumContent(album.mediaAlbum.toAlbumContentNavKey())
                    },
                coverImage = when (val result = downloadResult?.value) {
                    is DownloadPhotoResult.Success -> result.thumbnailFilePath
                    else -> null
                },
                title = album.title,
                placeholder = placeholder,
                errorPlaceholder = placeholder
            )
        }
    }

    if (showNewAlbumDialogEvent == triggered) {
        AddNewAlbumDialog(
            modifier = Modifier.testTag(ALBUMS_SCREEN_ADD_NEW_ALBUM_DIALOG),
            onDismiss = resetNewAlbumDialogEvent,
            onConfirm = addNewAlbum
        )
    }
}

const val ALBUMS_SCREEN_ALBUM_GRID_ITEM = "albums_tab_screen:album_grid_item"
const val ALBUMS_SCREEN_ADD_NEW_ALBUM_DIALOG = "albums_tab_screen:add_new_album_dialog"