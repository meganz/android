package mega.privacy.android.feature.photos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.decode.DataSource
import coil3.request.ImageRequest
import coil3.request.crossfade
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.modifiers.conditional
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.domain.entity.photos.thumbnail.MediaThumbnailRequest
import mega.privacy.android.icon.pack.IconPack

@Composable
fun ImagePhotosNode(
    thumbnailRequest: MediaThumbnailRequest,
    isSensitive: Boolean,
    isSelected: Boolean,
    shouldShowFavourite: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    BasicPhotosNode(
        modifier = modifier,
        thumbnailRequest = thumbnailRequest,
        isSensitive = isSensitive,
        isSelected = isSelected,
        shouldShowFavourite = shouldShowFavourite,
        enabled = enabled,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@Composable
fun VideoPhotosNode(
    duration: String,
    thumbnailRequest: MediaThumbnailRequest,
    isSensitive: Boolean,
    isSelected: Boolean,
    shouldShowFavourite: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Box(modifier = modifier) {
        BasicPhotosNode(
            modifier = Modifier.fillMaxSize(),
            thumbnailRequest = thumbnailRequest,
            isSensitive = isSensitive,
            isSelected = isSelected,
            shouldShowFavourite = shouldShowFavourite,
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick,
        )

        if (duration.isNotEmpty()) {
            MegaText(
                modifier = Modifier
                    .padding(bottom = 4.dp, end = 4.dp)
                    .background(
                        color = DSTokens.colors.background.surfaceTransparent.copy(
                            alpha = 0.7F
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .padding(
                        vertical = 2.dp,
                        horizontal = 4.dp
                    )
                    .align(Alignment.BottomEnd)
                    .testTag(VIDEO_PHOTOS_NODE_DURATION_TEXT_TAG),
                text = duration,
                style = MaterialTheme.typography.labelSmall,
                textColor = TextColor.OnColor
            )
        }
    }
}

@Composable
private fun BasicPhotosNode(
    thumbnailRequest: MediaThumbnailRequest,
    isSensitive: Boolean,
    isSelected: Boolean,
    shouldShowFavourite: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val request = remember(thumbnailRequest) {
        ImageRequest.Builder(context)
            .data(thumbnailRequest)
            .crossfade(enable = true)
            .build()
    }
    var contentScale by remember { mutableStateOf(ContentScale.Crop) }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(
                enabled = enabled,
                onClick = { onClick?.invoke() },
                onLongClick = { onLongClick?.invoke() }
            )
            .conditional(isSelected) {
                Modifier
                    .conditional(!enabled) {
                        this.alpha(0.5f)
                    }
                    .border(
                        width = 2.dp,
                        color = DSTokens.colors.border.strongSelected,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clip(RoundedCornerShape(4.dp))
            }
    ) {
        AsyncImage(
            modifier = Modifier
                .aspectRatio(1f)
                .background(color = DSTokens.colors.background.surface1)
                .conditional(!enabled) {
                    this.alpha(0.5f)
                }
                .conditional(isSensitive) {
                    this
                        .alpha(0.5f)
                        .blur(16.dp)
                }
                .testTag(BASIC_PHOTOS_NODE_IMAGE_THUMBNAIL_FILE_TAG),
            model = request,
            onState = { state ->
                contentScale =
                    if (state is AsyncImagePainter.State.Success && state.result.dataSource == DataSource.MEMORY) {
                        ContentScale.Inside
                    } else {
                        ContentScale.Crop
                    }
            },
            contentDescription = null,
            contentScale = contentScale,
        )

        if (shouldShowFavourite) {
            MegaIcon(
                modifier = Modifier
                    .padding(top = 4.dp, end = 4.dp)
                    .background(
                        color = DSTokens.colors.background.surfaceTransparent,
                        shape = RoundedCornerShape(2.dp)
                    )
                    .padding(2.dp)
                    .align(Alignment.TopEnd)
                    .testTag(BASIC_PHOTOS_NODE_FAVOURITE_ICON_TAG),
                painter = rememberVectorPainter(IconPack.Small.Thin.Solid.Heart),
                contentDescription = null,
                tint = IconColor.OnColor,
            )
        }

        if (isSelected) {
            MegaIcon(
                modifier = Modifier
                    .padding(top = 4.dp, start = 4.dp)
                    .background(
                        color = DSTokens.colors.background.surfaceTransparent,
                        shape = RoundedCornerShape(2.dp)
                    )
                    .padding(2.dp)
                    .align(Alignment.TopStart),
                painter = rememberVectorPainter(IconPack.Small.Thin.Outline.Check),
                contentDescription = "check icon",
                tint = IconColor.OnColor,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ImagePhotosNodePreview(
    @PreviewParameter(BooleanProvider::class) isTrue: Boolean,
) {
    AndroidThemeForPreviews {
        ImagePhotosNode(
            modifier = Modifier.size(137.dp),
            thumbnailRequest = MediaThumbnailRequest(
                id = 1L,
                isPreview = false,
                thumbnailFilePath = "node.photo.thumbnailFilePath",
                previewFilePath = "node.photo.previewFilePath",
                isPublicNode = false,
                fileExtension = "jpg"
            ),
            isSensitive = false,
            isSelected = isTrue,
            shouldShowFavourite = isTrue
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideoPhotosNodePreview(
    @PreviewParameter(BooleanProvider::class) isTrue: Boolean,
) {
    AndroidThemeForPreviews {
        VideoPhotosNode(
            modifier = Modifier.size(137.dp),
            duration = "2.50",
            thumbnailRequest = MediaThumbnailRequest(
                id = 1L,
                isPreview = false,
                thumbnailFilePath = "node.photo.thumbnailFilePath",
                previewFilePath = "node.photo.previewFilePath",
                isPublicNode = false,
                fileExtension = "mov"
            ),
            isSensitive = false,
            isSelected = isTrue,
            shouldShowFavourite = isTrue
        )
    }
}

@CombinedThemePreviews
@Composable
private fun BasicPhotosNodePreview(
    @PreviewParameter(BooleanProvider::class) isTrue: Boolean,
) {
    AndroidThemeForPreviews {
        BasicPhotosNode(
            modifier = Modifier.size(137.dp),
            thumbnailRequest = MediaThumbnailRequest(
                id = 1L,
                isPreview = false,
                thumbnailFilePath = "node.photo.thumbnailFilePath",
                previewFilePath = "node.photo.previewFilePath",
                isPublicNode = false,
                fileExtension = "jpg"
            ),
            isSensitive = false,
            isSelected = isTrue,
            shouldShowFavourite = isTrue
        )
    }
}

internal const val VIDEO_PHOTOS_NODE_DURATION_TEXT_TAG =
    "video_photos_node:text_duration"
internal const val BASIC_PHOTOS_NODE_IMAGE_THUMBNAIL_FILE_TAG =
    "basic_photos_node:image_thumbnail_file"
internal const val BASIC_PHOTOS_NODE_FAVOURITE_ICON_TAG =
    "basic_photos_node:icon_favourite"
