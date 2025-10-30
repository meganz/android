package mega.privacy.android.feature.photos.presentation.albums.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.domain.entity.photos.Photo

private val gap = 1.dp

@Composable
internal fun AlbumContentHighlightStart(
    size: Dp,
    photos: List<Photo>,
    selectedPhotos: Set<Photo>,
    shouldApplySensitiveMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: (Photo) -> Unit = {},
    onLongPress: (Photo) -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        AlbumPhotoContainer(
            onClick = onClick,
            onLongPress = onLongPress,
            albumPhotoView = {
                AlbumPhotoItem(
                    width = size * 2,
                    height = size * 2 + gap,
                    photo = photos.first(),
                    isPreview = true,
                    isSensitive = shouldApplySensitiveMode && (photos.first().isSensitive || photos.first().isSensitiveInherited),
                )
            },
            photo = photos.first(),
            isSelected = photos.first() in selectedPhotos
        )
        if (photos.size >= 2) {
            Column(verticalArrangement = Arrangement.SpaceBetween) {
                AlbumPhotoContainer(
                    onClick = onClick,
                    onLongPress = onLongPress,
                    albumPhotoView = {
                        AlbumPhotoItem(
                            width = size,
                            height = size,
                            photo = photos[1],
                            isSensitive = shouldApplySensitiveMode && (photos[1].isSensitive || photos[1].isSensitiveInherited),
                        )
                    },
                    photo = photos[1],
                    isSelected = photos[1] in selectedPhotos
                )
                if (photos.size == 3) {
                    Spacer(modifier = Modifier.height(1.dp))

                    AlbumPhotoContainer(
                        onClick = onClick,
                        onLongPress = onLongPress,
                        albumPhotoView = {
                            AlbumPhotoItem(
                                width = size,
                                height = size,
                                photo = photos[2],
                                isSensitive = shouldApplySensitiveMode && (photos[2].isSensitive || photos[2].isSensitiveInherited),
                            )
                        },
                        photo = photos[2],
                        isSelected = photos[2] in selectedPhotos
                    )
                }
            }
        }
    }
}

@Composable
internal fun AlbumContentUniform(
    size: Dp,
    photos: List<Photo>,
    selectedPhotos: Set<Photo>,
    shouldApplySensitiveMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: (Photo) -> Unit = {},
    onLongPress: (Photo) -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        AlbumPhotoContainer(
            onClick = onClick,
            onLongPress = onLongPress,
            albumPhotoView = {
                AlbumPhotoItem(
                    width = size,
                    height = size,
                    photo = photos.first(),
                    isSensitive = shouldApplySensitiveMode && (photos.first().isSensitive || photos.first().isSensitiveInherited),
                )
            },
            photo = photos.first(),
            isSelected = photos.first() in selectedPhotos
        )
        if (photos.size >= 2) {
            AlbumPhotoContainer(
                onClick = onClick,
                onLongPress = onLongPress,
                albumPhotoView = {
                    AlbumPhotoItem(
                        width = size,
                        height = size,
                        photo = photos[1],
                        isSensitive = shouldApplySensitiveMode && (photos[1].isSensitive || photos[1].isSensitiveInherited),
                    )
                },
                photo = photos[1],
                isSelected = photos[1] in selectedPhotos,
            )
            if (photos.size == 2) {
                Spacer(modifier = Modifier.size(size))
            }
        }
        if (photos.size == 3) {
            AlbumPhotoContainer(
                onClick = onClick,
                onLongPress = onLongPress,
                albumPhotoView = {
                    AlbumPhotoItem(
                        width = size,
                        height = size,
                        photo = photos[2],
                        isSensitive = shouldApplySensitiveMode && (photos[2].isSensitive || photos[2].isSensitiveInherited),
                    )
                },
                photo = photos[2],
                isSelected = photos[2] in selectedPhotos
            )
        }
    }
}

@Composable
internal fun AlbumContentHighlightEnd(
    size: Dp, photos: List<Photo>,
    selectedPhotos: Set<Photo>,
    shouldApplySensitiveMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: (Photo) -> Unit = {},
    onLongPress: (Photo) -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            AlbumPhotoContainer(
                onClick = onClick,
                onLongPress = onLongPress,
                albumPhotoView = {
                    AlbumPhotoItem(
                        width = size,
                        height = size,
                        photo = photos.first(),
                        isSensitive = shouldApplySensitiveMode && (photos.first().isSensitive || photos.first().isSensitiveInherited),
                    )
                },
                photo = photos.first(),
                isSelected = photos.first() in selectedPhotos
            )

            if (photos.size == 3) {
                Spacer(modifier = Modifier.height(1.dp))

                AlbumPhotoContainer(
                    onClick = onClick,
                    onLongPress = onLongPress,
                    albumPhotoView = {
                        AlbumPhotoItem(
                            width = size,
                            height = size,
                            photo = photos[2],
                            isSensitive = shouldApplySensitiveMode && (photos[2].isSensitive || photos[2].isSensitiveInherited),
                        )
                    },
                    photo = photos[2],
                    isSelected = photos[2] in selectedPhotos
                )
            }
        }
        if (photos.size >= 2) {
            AlbumPhotoContainer(
                onClick = onClick,
                onLongPress = onLongPress,
                albumPhotoView = {
                    AlbumPhotoItem(
                        width = size * 2,
                        height = size * 2 + gap,
                        photo = photos[1],
                        isPreview = true,
                        isSensitive = shouldApplySensitiveMode && (photos[1].isSensitive || photos[1].isSensitiveInherited),
                    )
                },
                photo = photos[1],
                isSelected = photos[1] in selectedPhotos
            )
        }
    }
}
