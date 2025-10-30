package mega.privacy.android.feature.photos.presentation.albums.view

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
import mega.privacy.android.domain.entity.photos.Photo

@Composable
fun AlbumDynamicContentGrid(
    lazyListState: LazyListState,
    photos: List<Photo>,
    smallWidth: Dp,
    selectedPhotos: Set<Photo>,
    modifier: Modifier = Modifier,
    endSpacing: Dp = 0.dp,
    shouldApplySensitiveMode: Boolean = false,
    onClick: (Photo) -> Unit = {},
    onLongPress: (Photo) -> Unit = {},
) {
    val albumContentLayouts = remember(photos) {
        photos.chunked(3).mapIndexed { i, chunkedPhotos ->
            if (i % 4 == 0) {
                AlbumContentLayout.HighlightStart(chunkedPhotos)
            } else if (i % 4 == 1 || i % 4 == 3) {
                AlbumContentLayout.Uniform(chunkedPhotos)
            } else {
                AlbumContentLayout.HighlightEnd(chunkedPhotos)
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        this.items(
            albumContentLayouts,
            key = { it.key },
        ) { layout ->
            when (layout) {
                is AlbumContentLayout.HighlightStart -> {
                    AlbumContentHighlightStart(
                        size = smallWidth,
                        photos = layout.content,
                        onClick = onClick,
                        onLongPress = onLongPress,
                        selectedPhotos = selectedPhotos,
                        shouldApplySensitiveMode = shouldApplySensitiveMode,
                    )
                }

                is AlbumContentLayout.Uniform -> {
                    AlbumContentUniform(
                        size = smallWidth,
                        photos = layout.content,
                        onClick = onClick,
                        onLongPress = onLongPress,
                        selectedPhotos = selectedPhotos,
                        shouldApplySensitiveMode = shouldApplySensitiveMode,
                    )
                }

                is AlbumContentLayout.HighlightEnd -> {
                    AlbumContentHighlightEnd(
                        size = smallWidth,
                        photos = layout.content,
                        onClick = onClick,
                        onLongPress = onLongPress,
                        selectedPhotos = selectedPhotos,
                        shouldApplySensitiveMode = shouldApplySensitiveMode,
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(endSpacing))
        }
    }
}