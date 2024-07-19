package mega.privacy.android.app.presentation.meeting.chat.view.message.meta

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import mega.privacy.android.app.services.GiphyService
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGifInfo
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.GiphyMessagePlaceHolder
import mega.privacy.android.shared.original.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import timber.log.Timber


/**
 * Giphy message view
 *
 * @param gifInfo
 * @param autoPlay
 * @param modifier
 * @param title
 * @param onLoaded
 * @param onError
 */
@Composable
fun GiphyMessageView(
    gifInfo: ChatGifInfo,
    autoPlay: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    onLoaded: () -> Unit = {},
    onError: () -> Unit = {},
) {
    val context = LocalContext.current
    val maxWidth = 256

    with(gifInfo) {
        val actualWidth = minOf(maxWidth, width)
        val actualHeight: Int = actualWidth * height / width

        var showPlaceHolder by remember { mutableStateOf(false) }


        Box(
            modifier = modifier
                .size(width = actualWidth.dp, height = actualHeight.dp)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            if (autoPlay) {
                val url = webpSrc?.toGiphyUri()?.toString()
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .crossfade(true)
                        .data(url)
                        .build(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onState = { state ->
                        when (state) {
                            is AsyncImagePainter.State.Success -> onLoaded()
                            is AsyncImagePainter.State.Error -> onError()
                            else -> {}
                        }
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
}

/**
 * Gets the original src of a Giphy by replacing GIPHY_URL to the endpoint.
 *
 * @return The final src with real endpoint.
 */
fun String.toGiphyUri(): Uri? =
    when {
        !isNullOrEmpty() && this.contains(GiphyService.GIPHY_URL) -> {
            Uri.parse(this.replace(GiphyService.GIPHY_URL, GiphyService.BASE_URL))
        }

        !isNullOrEmpty() -> {
            Timber.e("Wrong giphyUri: $this")
            Uri.parse(this)
        }

        else -> {
            Timber.e("Wrong giphyUri: $this")
            null
        }
    }

