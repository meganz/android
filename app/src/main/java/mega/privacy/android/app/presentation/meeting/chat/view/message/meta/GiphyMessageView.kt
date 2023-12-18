package mega.privacy.android.app.presentation.meeting.chat.view.message.meta

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import mega.privacy.android.core.ui.controls.chat.messages.GiphyMessagePlaceHolder

/**
 * Giphy message view
 *
 * @param url Giphy url
 * @param width
 * @param height
 * @param title option text title of the giphy image
 */
@Composable
fun GiphyMessageView(
    url: String,
    width: Int,
    height: Int,
    title: String? = null,
) {
    val maxWidth = 256

    val actualWidth = minOf(maxWidth, width)
    val actualHeight: Int = actualWidth * height / width

    var showPlaceHolder by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = actualWidth.dp, height = actualHeight.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .crossfade(true)
                .data(url)
                .build(),
            contentDescription = title,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .size(width = actualWidth.dp, height = actualHeight.dp),
            contentScale = ContentScale.Crop,
            onState = { state -> showPlaceHolder = state !is AsyncImagePainter.State.Success }
        )
        AnimatedVisibility(visible = showPlaceHolder) {
            GiphyMessagePlaceHolder(width = actualWidth, height = actualHeight)
        }
    }
}