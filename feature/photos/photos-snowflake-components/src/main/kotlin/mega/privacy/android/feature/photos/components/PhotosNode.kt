package mega.privacy.android.feature.photos.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.modifiers.conditional
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack

@Immutable
sealed interface PhotosNodeThumbnailData {

    data class Placeholder(@DrawableRes val imageResId: Int) : PhotosNodeThumbnailData

    data class File(
        val path: String,
        val isSensitive: Boolean,
        val alpha: Float = DefaultAlpha,
    ) : PhotosNodeThumbnailData
}

@Composable
fun ImagePhotosNode(
    thumbnailData: PhotosNodeThumbnailData,
    isSelected: Boolean,
    shouldShowFavourite: Boolean,
    modifier: Modifier = Modifier,
) {
    BasicPhotosNode(
        modifier = modifier,
        thumbnailData = thumbnailData,
        isSelected = isSelected,
        shouldShowFavourite = shouldShowFavourite
    )
}

@Composable
fun VideoPhotosNode(
    duration: String,
    thumbnailData: PhotosNodeThumbnailData,
    isSelected: Boolean,
    shouldShowFavourite: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        BasicPhotosNode(
            modifier = Modifier.fillMaxSize(),
            thumbnailData = thumbnailData,
            isSelected = isSelected,
            shouldShowFavourite = shouldShowFavourite
        )

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

@Composable
private fun BasicPhotosNode(
    thumbnailData: PhotosNodeThumbnailData,
    isSelected: Boolean,
    shouldShowFavourite: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .conditional(isSelected) {
                Modifier.border(
                    width = 2.dp,
                    color = DSTokens.colors.border.strongSelected,
                    shape = RoundedCornerShape(4.dp)
                )
            }
    ) {
        when (thumbnailData) {
            is PhotosNodeThumbnailData.Placeholder -> {
                Image(
                    modifier = Modifier
                        .height(172.dp)
                        .fillMaxWidth()
                        .padding(vertical = 34.dp)
                        .testTag(BASIC_PHOTOS_NODE_IMAGE_THUMBNAIL_PLACEHOLDER_TAG),
                    painter = painterResource(thumbnailData.imageResId),
                    contentDescription = "default icon",
                    contentScale = ContentScale.Fit,
                )
            }

            is PhotosNodeThumbnailData.File -> {
                AsyncImage(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .conditional(thumbnailData.isSensitive) {
                            this
                                .alpha(0.5f)
                                .blur(16.dp)
                        }
                        .testTag(BASIC_PHOTOS_NODE_IMAGE_THUMBNAIL_FILE_TAG),
                    model = ImageRequest
                        .Builder(LocalContext.current)
                        .data(thumbnailData.path)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    placeholder = ColorPainter(DSTokens.colors.background.surface2),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = thumbnailData.alpha
                )
            }
        }

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
            Checkbox(
                modifier = Modifier.padding(top = 1.dp, start = 1.dp),
                checked = true,
                onCheckStateChanged = {},
                tapTargetArea = false,
                clickable = false,
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
            thumbnailData = PhotosNodeThumbnailData.Placeholder(mega.privacy.android.icon.pack.R.drawable.ic_usp_2),
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
            thumbnailData = PhotosNodeThumbnailData.Placeholder(mega.privacy.android.icon.pack.R.drawable.ic_usp_2),
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
            thumbnailData = PhotosNodeThumbnailData.Placeholder(mega.privacy.android.icon.pack.R.drawable.ic_usp_2),
            isSelected = isTrue,
            shouldShowFavourite = isTrue
        )
    }
}

internal const val VIDEO_PHOTOS_NODE_DURATION_TEXT_TAG =
    "video_photos_node:text_duration"
internal const val BASIC_PHOTOS_NODE_IMAGE_THUMBNAIL_PLACEHOLDER_TAG =
    "basic_photos_node:image_thumbnail_placeholder"
internal const val BASIC_PHOTOS_NODE_IMAGE_THUMBNAIL_FILE_TAG =
    "basic_photos_node:image_thumbnail_file"
internal const val BASIC_PHOTOS_NODE_FAVOURITE_ICON_TAG =
    "basic_photos_node:icon_favourite"
