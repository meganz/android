package mega.privacy.android.app.presentation.meeting.chat.view.message.meta

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.chat.messages.GiphyMessagePlaceHolder
import mega.privacy.android.core.ui.controls.progressindicator.MegaCircularProgressIndicator


/**
 * Giphy message view
 *
 * @param url Giphy url
 * @param width
 * @param height
 * @param title option text title of the giphy image
 * @param autoPlayGif whether gif should be auto played
 * @param onLoaded callback when gif is loaded
 * @param onClick callback when user clicks the loaded gif
 */
@Composable
fun GiphyMessageView(
    url: String,
    width: Int,
    height: Int,
    autoPlayGif: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    onLoaded: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    val maxWidth = 256

    val actualWidth = minOf(maxWidth, width)
    val actualHeight: Int = actualWidth * height / width

    var showPlaceHolder by remember { mutableStateOf(false) }
    var autoPlay: Boolean by remember { mutableStateOf(autoPlayGif) }

    Box(
        modifier = modifier
            .size(width = actualWidth.dp, height = actualHeight.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (!autoPlay) {
                    autoPlay = true
                } else {
                    onClick()
                }
            }
    ) {
        if (autoPlay) {
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
                onState = { state ->
                    if (state is AsyncImagePainter.State.Success) onLoaded()
                    showPlaceHolder = state !is AsyncImagePainter.State.Success
                }
            )
            AnimatedVisibility(visible = showPlaceHolder) {
                GiphyMessagePlaceHolder(width = actualWidth, height = actualHeight) {
                    MegaCircularProgressIndicator(modifier = Modifier.size(48.dp))
                }
            }
        } else {
            GiphyMessagePlaceHolder(width = actualWidth, height = actualHeight) {
                Image(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_gif_message),
                    contentDescription = null
                )
            }
        }

    }
}

