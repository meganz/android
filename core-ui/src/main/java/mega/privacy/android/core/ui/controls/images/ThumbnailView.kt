package mega.privacy.android.core.ui.controls.images

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import mega.privacy.android.icon.pack.R
import mega.privacy.android.core.ui.theme.AndroidTheme
import java.io.File

/**
 * Thumbnail View for NodesView
 * @param modifier [Modifier]
 * @param imageFile File
 * @param contentDescription Content Description for image,
 * @param defaultImage in case of imageFile is null
 * @param contentScale [ContentScale]
 */
@Composable
fun ThumbnailView(
    contentDescription: String?,
    imageFile: File?,
    @DrawableRes defaultImage: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    ThumbnailView(
        contentDescription = contentDescription,
        data = imageFile,
        defaultImage = defaultImage,
        modifier = modifier,
        contentScale = contentScale,
    )
}

/**
 * Thumbnail view
 *
 * @param contentDescription content description
 * @param data any data [File], [Uri], [Bitmap], [ThumbnailRequest]
 * @param defaultImage default image
 * @param modifier
 * @param contentScale content scale
 */
@Composable
fun ThumbnailView(
    contentDescription: String?,
    data: Any?,
    @DrawableRes defaultImage: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    var padding by remember { mutableStateOf(0.dp) }
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
            contentScale = contentScale,
            modifier = modifier
                .aspectRatio(1f)
                .padding(padding)
                .clip(RoundedCornerShape(cornerRadius)),
            onError = {
                padding = 0.dp
                cornerRadius = 0.dp
            },
            onSuccess = {
                padding = 4.dp
                cornerRadius = 4.dp
            }
        )
    } ?: run {
        Image(
            modifier = modifier,
            painter = painterResource(id = defaultImage),
            contentDescription = contentDescription,
            contentScale = contentScale,
        )
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ThumbnailViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ThumbnailView(
            contentDescription = "image",
            imageFile = null as File?,
            defaultImage = R.drawable.ic_image_medium_solid,
        )
    }
}
