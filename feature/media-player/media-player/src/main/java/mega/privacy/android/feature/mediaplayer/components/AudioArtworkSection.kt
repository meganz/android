package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData
import mega.privacy.android.icon.pack.R as iconPackR

/**
 * The artwork of the playing track, drawn in three layers.
 *
 * The default audio icon is at the bottom, the thumbnail over it, and [artworkUri] or
 * [previewData] over that. The thumbnail is already in the image cache from the screen the
 * player was opened from, so it appears at once and holds the space while the sharper image
 * is fetched.
 */
@Composable
internal fun AudioArtworkSection(
    artworkUri: String?,
    thumbnailData: ThumbnailData?,
    previewData: ThumbnailData?,
    modifier: Modifier = Modifier,
) {
    val topModel: Any? = artworkUri ?: previewData

    // Tracked per image so that a failure of one does not hide an image the other has drawn.
    var thumbnailLoaded by remember(thumbnailData) { mutableStateOf(false) }
    var topLoaded by remember(topModel) { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        if (!thumbnailLoaded && !topLoaded) {
            Image(
                painter = painterResource(iconPackR.drawable.ic_audio_medium_solid),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }

        if (thumbnailData != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailData)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { thumbnailLoaded = true },
                onError = { thumbnailLoaded = false },
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }

        if (topModel != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(topModel)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { topLoaded = true },
                onError = { topLoaded = false },
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}
