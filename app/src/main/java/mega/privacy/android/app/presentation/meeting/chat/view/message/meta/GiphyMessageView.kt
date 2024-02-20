package mega.privacy.android.app.presentation.meeting.chat.view.message.meta

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
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
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openGiphyViewerActivity
import mega.privacy.android.app.services.GiphyService
import mega.privacy.android.core.ui.controls.chat.messages.GiphyMessagePlaceHolder
import mega.privacy.android.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGifInfo
import timber.log.Timber


/**
 * Giphy message view
 *
 * @param gifInfo giphy info
 * @param title option text title of the giphy image
 * @param autoPlayGif whether gif should be auto played
 * @param onLoaded callback when gif is loaded
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GiphyMessageView(
    gifInfo: ChatGifInfo,
    autoPlayGif: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    onLoaded: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val maxWidth = 256

    with(gifInfo) {
        val actualWidth = minOf(maxWidth, width)
        val actualHeight: Int = actualWidth * height / width

        var showPlaceHolder by remember { mutableStateOf(false) }
        var autoPlay: Boolean by remember { mutableStateOf(autoPlayGif) }

        Box(
            modifier = modifier
                .size(width = actualWidth.dp, height = actualHeight.dp)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = {
                        if (!autoPlay) {
                            autoPlay = true
                        } else {
                            openGiphyViewerActivity(context, gifInfo)
                        }
                    },
                    onLongClick = onLongClick
                )
        ) {
            if (autoPlay) {
                val url = webpSrc?.toUri()?.toString()
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
                            is AsyncImagePainter.State.Error -> autoPlay = false
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

private fun String.toUri(): Uri? =
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

