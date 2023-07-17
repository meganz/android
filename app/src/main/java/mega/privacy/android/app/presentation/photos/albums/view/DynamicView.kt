package mega.privacy.android.app.presentation.photos.albums.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.photos.albums.model.AlbumPhotoItem
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.domain.entity.photos.Photo

@Composable
internal fun DynamicView(
    lazyListState: LazyListState,
    photos: List<Photo>,
    smallWidth: Dp,
    photoDownload: PhotoDownload,
    onClick: (Photo) -> Unit = {},
    onLongPress: (Photo) -> Unit = {},
    selectedPhotos: Set<Photo>,
    endSpacing: Dp = 0.dp,
) {
    val dynamicList = remember(photos) {
        photos.chunked(3).mapIndexed { i, list ->
            if (i % 4 == 0) {
                AlbumPhotoItem.BigSmall2Item(
                    list
                )
            } else if (i % 4 == 1 || i % 4 == 3) {
                AlbumPhotoItem.Small3Item(
                    list
                )
            } else {
                AlbumPhotoItem.Small2BigItem(
                    list
                )
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        this.items(
            dynamicList,
            key = { it.key },
        ) { item ->
            when (item) {
                is AlbumPhotoItem.BigSmall2Item -> {
                    PhotosBig2SmallItems(
                        size = smallWidth,
                        photos = item.photos,
                        photoDownload = photoDownload,
                        onClick = onClick,
                        onLongPress = onLongPress,
                        selectedPhotos = selectedPhotos
                    )
                }

                is AlbumPhotoItem.Small3Item -> {
                    Photos3SmallItems(
                        size = smallWidth,
                        photos = item.photos,
                        downloadPhoto = photoDownload,
                        onClick = onClick,
                        onLongPress = onLongPress,
                        selectedPhotos = selectedPhotos
                    )
                }

                is AlbumPhotoItem.Small2BigItem -> {
                    Photos2SmallBigItems(
                        size = smallWidth,
                        photos = item.photos,
                        downloadPhoto = photoDownload,
                        onClick = onClick,
                        onLongPress = onLongPress,
                        selectedPhotos = selectedPhotos
                    )
                }
            }
        }

        item {
            Spacer(
                modifier = Modifier.height(endSpacing),
            )
        }
    }
}