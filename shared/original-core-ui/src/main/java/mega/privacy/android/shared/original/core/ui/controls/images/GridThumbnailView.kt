package mega.privacy.android.shared.original.core.ui.controls.images

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import mega.privacy.android.icon.pack.R
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import java.io.File


/**
 * Thumbnail view
 *
 * @param contentDescription content description
 * @param data any data [File], Uri, Bitmap, ThumbnailRequest
 * @param defaultImage default image
 * @param modifier
 * @param contentScale content scale
 */
@Deprecated("Use the version from core-ui library")
@Composable
fun GridThumbnailView(
    contentDescription: String?,
    data: Any?,
    @DrawableRes defaultImage: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    var imageLoaded by remember { mutableStateOf(false) }
    var cornerRadius by remember { mutableStateOf(0.dp) }
    data?.let {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(data)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            placeholder = painterResource(id = defaultImage),
            error = painterResource(id = defaultImage),
            contentScale = if (imageLoaded) contentScale else ContentScale.Fit,
            modifier = modifier
                .clip(RoundedCornerShape(cornerRadius))
                .padding(vertical = if (imageLoaded) 0.dp else 34.dp),
            onError = {
                imageLoaded = false
                cornerRadius = 0.dp
            },
            onSuccess = {
                imageLoaded = true
                cornerRadius = 4.dp
            }
        )
    } ?: Image(
        modifier = modifier
            .height(172.dp)
            .fillMaxWidth()
            .padding(vertical = 34.dp),
        painter = painterResource(id = defaultImage),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
    )
}

@Deprecated("Use the version from core-ui library")
@Composable
fun GridThumbnailView(
    contentDescription: String?,
    data: Any?,
    @DrawableRes defaultImage: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onSuccess: (Modifier) -> Modifier,
) {
    data?.let {
        var imageLoaded by remember { mutableStateOf(false) }
        var cornerRadius by remember { mutableStateOf(0.dp) }
        var finalModifier by remember { mutableStateOf(modifier) }

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(data)
                .crossfade(true)
                .error(defaultImage)
                .placeholder(defaultImage)
                .build(),
            contentDescription = contentDescription,
            contentScale = if (imageLoaded) contentScale else ContentScale.Fit,
            modifier = finalModifier
                .clip(RoundedCornerShape(cornerRadius))
                .padding(vertical = if (imageLoaded) 0.dp else 34.dp),
            onSuccess = {
                finalModifier = onSuccess(modifier)
                imageLoaded = true
                cornerRadius = 4.dp
            },
            onError = {
                imageLoaded = false
                cornerRadius = 0.dp
            }
        )
    } ?: Image(
        modifier = modifier
            .height(172.dp)
            .fillMaxWidth()
            .padding(vertical = 34.dp),
        painter = painterResource(id = defaultImage),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
    )
}

@CombinedThemePreviews
@Composable
private fun GridThumbnailViewPreview() {
    GridThumbnailView(
        contentDescription = "Thumbnail",
        data = null,
        defaultImage = R.drawable.ic_folder_medium_thin_outline,
        modifier = Modifier,
    )
}

@CombinedThemePreviews
@Composable
private fun GridThumbnailViewPreviewWithData() {
    GridThumbnailView(
        contentDescription = "Thumbnail",
        data = "https://www.mega.com/favicon.ico",
        defaultImage = R.drawable.ic_folder_medium_thin_outline,
        modifier = Modifier.size(172.dp),
    )
}
