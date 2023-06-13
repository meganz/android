package mega.privacy.android.app.presentation.view

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import mega.privacy.android.app.R
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
    imageFile?.let {
        Image(
            modifier = modifier
                .aspectRatio(1f),
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(it)
                    .crossfade(true)
                    .build(),
                placeholder = painterResource(id = R.drawable.ic_image_thumbnail),
                error = painterResource(id = R.drawable.ic_image_thumbnail),
            ),
            contentDescription = contentDescription,
            contentScale = contentScale,
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
            defaultImage = R.drawable.ic_image_thumbnail,
        )
    }
}
