package mega.privacy.android.app.presentation.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.domain.entity.node.TypedNode
import java.io.File

/**
 * Thumbnail View for NodesView
 * @param modifier [Modifier]
 * @param imageFile File
 * @param contentDescription Content Description for image,
 * @param node [NodeUIItem]
 * @param contentScale [ContentScale]
 */
@Composable
fun <T : TypedNode> ThumbnailView(
    contentDescription: String?,
    imageFile: File?,
    node: NodeUIItem<T>,
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
            painter = painterResource(id = MimeTypeList.typeForName(node.name).iconResourceId),
            contentDescription = contentDescription,
            contentScale = contentScale,
        )
    }
}