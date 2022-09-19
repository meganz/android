package mega.privacy.android.app.presentation.photos.albums.view

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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap.Companion.Square
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsViewState
import mega.privacy.android.app.presentation.photos.albums.model.titleId

@Composable
fun AlbumsView(
    albumsViewState: AlbumsViewState,
    openAlbum: (album: Album) -> Unit,
) {
    LazyVerticalGrid(
        contentPadding = PaddingValues(top = 8.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items = albumsViewState.albums) { album ->
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .clickable {
                        openAlbum(album)
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .fillMaxSize()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(album.thumbnail)
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
                        text = stringResource(id = album.titleId),
                        style = MaterialTheme.typography.subtitle2,
                        color = colorResource(id = R.color.black_white),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = album.itemCount.toString(),
                        style = MaterialTheme.typography.caption,
                        color = colorResource(id = R.color.grey_054_white_054),
                    )
                }
            }
        }
    }
}
