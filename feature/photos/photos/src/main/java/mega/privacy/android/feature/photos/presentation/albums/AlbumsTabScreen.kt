package mega.privacy.android.feature.photos.presentation.albums

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
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.feature.photos.R
import mega.privacy.android.feature.photos.components.AlbumGridItem

@Composable
fun AlbumsTabRoute(
    modifier: Modifier = Modifier,
    viewModel: AlbumsTabViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AlbumsTabScreen(uiState = uiState, modifier = modifier)
}

@Composable
fun AlbumsTabScreen(
    uiState: AlbumsTabUiState,
    modifier: Modifier = Modifier,
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
            key = { index ->
                val album = uiState.albums[index]
                when (album) {
                    is MediaAlbum.User -> album.id.id
                    is MediaAlbum.System -> album.id.albumName.hashCode()
                }
            },
            contentType = { index ->
                uiState.albums[index]::class
            }
        ) { index ->
            val album = uiState.albums[index]
            val title = when (album) {
                is MediaAlbum.User -> album.title
                is MediaAlbum.System -> album.id.albumName
            }

            // Todo add downloader `PhotoDownloaderViewModel`
            AlbumGridItem(
                modifier = Modifier
                    .testTag("$ALBUMS_SCREEN_ALBUM_GRID_ITEM:${index}"),
                coverImage = album.cover?.thumbnailFilePath,
                title = title,
                placeholder = placeholder,
                errorPlaceholder = placeholder
            )
        }
    }
}

const val ALBUMS_SCREEN_ALBUM_GRID_ITEM = "albums_tab_screen:album_grid_item"