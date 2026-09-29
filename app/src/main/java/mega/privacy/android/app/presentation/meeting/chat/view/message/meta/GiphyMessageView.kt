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
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import mega.privacy.android.app.R
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

    with(gifInfo) {
        val (actualWidth, actualHeight) = calculateGiphyDisplaySize(width, height)

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
 * Calculates the size in dp at which a giphy is shown in the chat.
 *
 * The dimensions come from the sender's message meta and cannot be trusted, so non-positive
 * values fall back to a square and the height is capped.
 *
 * @param width Width of the giphy.
 * @param height Height of the giphy.
 * @return The display width and height.
 */
internal fun calculateGiphyDisplaySize(width: Int, height: Int): Pair<Int, Int> {
    if (width <= 0 || height <= 0) return GIPHY_MAX_WIDTH to GIPHY_MAX_WIDTH

    val displayWidth = minOf(GIPHY_MAX_WIDTH, width)
    val displayHeight = (displayWidth.toLong() * height / width)
        .coerceIn(1L, GIPHY_MAX_HEIGHT.toLong())
        .toInt()
    return displayWidth to displayHeight
}

/**
 * Gets the original src of a Giphy by replacing GIPHY_URL to the endpoint.
 *
 * Only sources served by the MEGA Giphy endpoint are accepted.
 *
 * @return The final src with real endpoint, or null if the src does not point to it.
 */
fun String.toGiphyUri(): Uri? {
    val url = when {
        startsWith(GIPHY_URL) ->
            BASE_URL + removePrefix(GIPHY_URL)

        startsWith(BASE_URL) -> this
        else -> null
    }
    val uri = url?.let { Uri.parse(it) }
        ?.takeIf { it.scheme == GIPHY_SCHEME && it.host == GIPHY_HOST }
    if (uri == null) Timber.w("Rejected giphy src: $this")
    return uri
}

private const val GIPHY_MAX_WIDTH = 256

// Guards against a sender-supplied height turning one message into a huge bubble. Giphy sends
// fixed-height renditions of about 200px, so real GIFs never reach this cap.
private const val GIPHY_MAX_HEIGHT = 1024
private const val BASE_URL = "https://giphy.mega.nz/"
private const val GIPHY_URL = "giphy://"
private const val GIPHY_SCHEME = "https"
private const val GIPHY_HOST = "giphy.mega.nz"

