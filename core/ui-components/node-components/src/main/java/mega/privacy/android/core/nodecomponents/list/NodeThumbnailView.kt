package mega.privacy.android.core.nodecomponents.list.view

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import mega.android.core.ui.R
import mega.android.core.ui.modifiers.conditional
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews


/**
 * Thumbnail composable for Node item
 *
 * @param data Image data (URL, File, ThumbnailRequest, etc.)
 * @param defaultImage Default drawable resource to show when no data or on error
 * @param modifier Modifier to apply to the image
 * @param contentScale How the loaded image should be scaled within its bounds
 * @param contentDescription Content description for accessibility
 * @param blurImage Whether to apply blur effect to the loaded image (for sensitive content)
 * @param layoutType The layout type (Grid or List) to determine sizing behavior
 */
@Composable
fun NodeThumbnailView(
    data: Any?,
    @DrawableRes defaultImage: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String?,
    blurImage: Boolean = false,
    layoutType: ThumbnailLayoutType = ThumbnailLayoutType.Grid,
) {
    if (data == null) {
        // Show default image when no data is provided
        Image(
            painter = painterResource(id = defaultImage),
            contentDescription = contentDescription,
            modifier = modifier
                .size(layoutType.placeholderSize)
                .testTag(NODE_THUMBNAIL_PLACEHOLDER_TAG),
            contentScale = ContentScale.Fit,
        )
        return
    }

    val context = LocalContext.current
    val imageRequest = remember(data, context) {
        ImageRequest.Builder(context)
            .data(data)
            .crossfade(true)
            .error(defaultImage)
            .apply {
                if (layoutType == ThumbnailLayoutType.List) {
                    placeholder(defaultImage)
                }
            }
            .build()
    }
    val painter = rememberAsyncImagePainter(imageRequest)
    val state by painter.state.collectAsStateWithLifecycle()
    val imageModifier = remember(state, blurImage, layoutType) {
        when (state) {
            is AsyncImagePainter.State.Success -> {
                modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .conditional(blurImage) {
                        blur(16.dp)
                    }
                    .testTag(NODE_THUMBNAIL_IMAGE_TAG)
            }

            else -> {
                modifier
                    .size(layoutType.placeholderSize)
                    .testTag(NODE_THUMBNAIL_PLACEHOLDER_TAG)
            }
        }
    }

    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = imageModifier,
        contentScale = if (state is AsyncImagePainter.State.Success) {
            contentScale
        } else {
            ContentScale.Fit
        }
    )
}

/**
 * Layout type for thumbnails with associated size information
 *
 * @property placeholderSize The size of the placeholder/error icon in Dp
 */
sealed class ThumbnailLayoutType(val placeholderSize: Dp) {
    object Grid : ThumbnailLayoutType(72.dp)

    object List : ThumbnailLayoutType(32.dp)
}

/**
 * Test tags for NodeThumbnailView components
 */
const val NODE_THUMBNAIL_PLACEHOLDER_TAG = "node_thumbnail_placeholder"
const val NODE_THUMBNAIL_IMAGE_TAG = "node_thumbnail_image"

@CombinedThemePreviews
@Composable
private fun NodeThumbnailViewGridPreview() {
    NodeThumbnailView(
        contentDescription = "Thumbnail",
        data = null,
        defaultImage = R.drawable.illustration_mega_anniversary,
        modifier = Modifier,
        layoutType = ThumbnailLayoutType.Grid
    )
}

@CombinedThemePreviews
@Composable
private fun NodeThumbnailViewListPreview() {
    AndroidThemeForPreviews {
        NodeThumbnailView(
            contentDescription = "Thumbnail",
            data = null,
            defaultImage = mega.privacy.android.icon.pack.R.drawable.illustration_notification_permission,
            modifier = Modifier,
            layoutType = ThumbnailLayoutType.List
        )
    }
}

@CombinedThemePreviews
@Composable
private fun NodeThumbnailViewGridWithDataPreview() {
    NodeThumbnailView(
        contentDescription = "Thumbnail",
        data = "https://www.mega.com/favicon.ico",
        defaultImage = mega.privacy.android.icon.pack.R.drawable.illustration_notification_permission,
        layoutType = ThumbnailLayoutType.Grid
    )
} 