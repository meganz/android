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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import mega.privacy.android.feature.photos.model.PhotoUiState

private val gap = 1.dp

@Composable
internal fun AlbumContentHighlightStart(
    size: Dp,
    photos: ImmutableList<PhotoUiState>,
    selectedPhotos: ImmutableSet<PhotoUiState>,
    shouldApplySensitiveMode: Boolean,
    isPublicAlbumPhoto: Boolean,
    modifier: Modifier = Modifier,
    onClick: (PhotoUiState) -> Unit = {},
    onLongPress: (PhotoUiState) -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        val firstPhoto = photos.first()

        AlbumPhotoItem(
            width = size * 2,
            height = size * 2 + gap,
            photo = firstPhoto,
            isPreview = true,
            isSensitive = shouldApplySensitiveMode && (firstPhoto.isSensitive || firstPhoto.isSensitiveInherited),
            isSelected = firstPhoto in selectedPhotos,
            isPublicAlbumPhoto = isPublicAlbumPhoto,
            onClick = onClick,
            onLongPress = onLongPress,
        )
        if (photos.size >= 2) {
            val secondPhoto = photos[1]
            Column(verticalArrangement = Arrangement.SpaceBetween) {
                AlbumPhotoItem(
                    width = size,
                    height = size,
                    photo = secondPhoto,
                    isSensitive = shouldApplySensitiveMode && (secondPhoto.isSensitive || secondPhoto.isSensitiveInherited),
                    isSelected = secondPhoto in selectedPhotos,
                    isPublicAlbumPhoto = isPublicAlbumPhoto,
                    onClick = onClick,
                    onLongPress = onLongPress,
                )

                if (photos.size == 3) {
                    val thirdPhoto = photos[2]

                    Spacer(modifier = Modifier.height(1.dp))

                    AlbumPhotoItem(
                        width = size,
                        height = size,
                        photo = thirdPhoto,
                        isSensitive = shouldApplySensitiveMode && (thirdPhoto.isSensitive || thirdPhoto.isSensitiveInherited),
                        isSelected = thirdPhoto in selectedPhotos,
                        isPublicAlbumPhoto = isPublicAlbumPhoto,
                        onClick = onClick,
                        onLongPress = onLongPress,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AlbumContentUniform(
    size: Dp,
    photos: ImmutableList<PhotoUiState>,
    selectedPhotos: ImmutableSet<PhotoUiState>,
    shouldApplySensitiveMode: Boolean,
    isPublicAlbumPhoto: Boolean,
    modifier: Modifier = Modifier,
    onClick: (PhotoUiState) -> Unit = {},
    onLongPress: (PhotoUiState) -> Unit = {},
) {
    val firstPhoto = photos.first()
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        AlbumPhotoItem(
            width = size,
            height = size,
            photo = firstPhoto,
            isSensitive = shouldApplySensitiveMode && (firstPhoto.isSensitive || firstPhoto.isSensitiveInherited),
            isSelected = firstPhoto in selectedPhotos,
            isPublicAlbumPhoto = isPublicAlbumPhoto,
            onClick = onClick,
            onLongPress = onLongPress,
        )
        if (photos.size >= 2) {
            val secondPhoto = photos[1]
            AlbumPhotoItem(
                width = size,
                height = size,
                photo = secondPhoto,
                isSensitive = shouldApplySensitiveMode && (secondPhoto.isSensitive || secondPhoto.isSensitiveInherited),
                isSelected = secondPhoto in selectedPhotos,
                isPublicAlbumPhoto = isPublicAlbumPhoto,
                onClick = onClick,
                onLongPress = onLongPress,
            )
            if (photos.size == 2) {
                Spacer(modifier = Modifier.size(size))
            }
        }
        if (photos.size == 3) {
            val thirdPhoto = photos[2]
            AlbumPhotoItem(
                width = size,
                height = size,
                photo = thirdPhoto,
                isSensitive = shouldApplySensitiveMode && (thirdPhoto.isSensitive || thirdPhoto.isSensitiveInherited),
                isSelected = thirdPhoto in selectedPhotos,
                isPublicAlbumPhoto = isPublicAlbumPhoto,
                onClick = onClick,
                onLongPress = onLongPress,
            )
        }
    }
}

@Composable
internal fun AlbumContentHighlightEnd(
    size: Dp,
    photos: ImmutableList<PhotoUiState>,
    selectedPhotos: ImmutableSet<PhotoUiState>,
    shouldApplySensitiveMode: Boolean,
    isPublicAlbumPhoto: Boolean,
    modifier: Modifier = Modifier,
    onClick: (PhotoUiState) -> Unit = {},
    onLongPress: (PhotoUiState) -> Unit = {},
) {
    val firstPhoto = photos.first()
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            AlbumPhotoItem(
                width = size,
                height = size,
                photo = firstPhoto,
                isSensitive = shouldApplySensitiveMode && (firstPhoto.isSensitive || firstPhoto.isSensitiveInherited),
                isSelected = firstPhoto in selectedPhotos,
                isPublicAlbumPhoto = isPublicAlbumPhoto,
                onClick = onClick,
                onLongPress = onLongPress,
            )

            if (photos.size == 3) {
                val thirdPhoto = photos[2]
                Spacer(modifier = Modifier.height(1.dp))

                AlbumPhotoItem(
                    width = size,
                    height = size,
                    photo = thirdPhoto,
                    isSensitive = shouldApplySensitiveMode && (thirdPhoto.isSensitive || thirdPhoto.isSensitiveInherited),
                    isSelected = thirdPhoto in selectedPhotos,
                    isPublicAlbumPhoto = isPublicAlbumPhoto,
                    onClick = onClick,
                    onLongPress = onLongPress,
                )
            }
        }
        if (photos.size >= 2) {
            val secondPhoto = photos[1]
            AlbumPhotoItem(
                width = size * 2,
                height = size * 2 + gap,
                photo = secondPhoto,
                isPreview = true,
                isSensitive = shouldApplySensitiveMode && (secondPhoto.isSensitive || secondPhoto.isSensitiveInherited),
                isSelected = secondPhoto in selectedPhotos,
                isPublicAlbumPhoto = isPublicAlbumPhoto,
                onClick = onClick,
                onLongPress = onLongPress,
            )
        }
    }
}
