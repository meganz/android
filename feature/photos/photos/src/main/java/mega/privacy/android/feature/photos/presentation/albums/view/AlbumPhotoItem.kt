package mega.privacy.android.feature.photos.presentation.albums.view

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import mega.privacy.android.domain.entity.photos.thumbnail.MediaThumbnailRequest
import mega.privacy.android.feature.photos.components.ImagePhotosNode
import mega.privacy.android.feature.photos.components.VideoPhotosNode
import mega.privacy.android.feature.photos.model.PhotoUiState

@Composable
internal fun AlbumPhotoItem(
    photo: PhotoUiState,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    isPreview: Boolean = false,
    isSensitive: Boolean = false,
    isSelected: Boolean = false,
    isPublicAlbumPhoto: Boolean = false,
    onClick: (PhotoUiState) -> Unit = {},
    onLongPress: (PhotoUiState) -> Unit = {},
) {
    val request = remember(isPublicAlbumPhoto, photo) {
        MediaThumbnailRequest(
            id = photo.id,
            isPreview = isPreview,
            thumbnailFilePath = photo.thumbnailFilePath,
            previewFilePath = photo.previewFilePath,
            isPublicNode = false,
            fileExtension = photo.fileTypeInfo.extension,
            isPublicAlbumPhoto = isPublicAlbumPhoto
        )
    }
    val nodeModifier = modifier
        .width(width)
        .height(height)
        .combinedClickable(
            onClick = { onClick(photo) },
            onLongClick = { onLongPress(photo) }
        )

    when (photo) {
        is PhotoUiState.Image -> {
            ImagePhotosNode(
                modifier = nodeModifier,
                thumbnailRequest = request,
                isSensitive = isSensitive,
                isSelected = isSelected,
                shouldShowFavourite = photo.isFavourite
            )
        }

        is PhotoUiState.Video -> {
            VideoPhotosNode(
                modifier = nodeModifier,
                thumbnailRequest = request,
                isSensitive = isSensitive,
                isSelected = isSelected,
                shouldShowFavourite = photo.isFavourite,
                duration = photo.duration
            )
        }
    }
}