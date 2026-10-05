package mega.privacy.android.feature.mediaplayer.videoplayer.presentation

import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Renders the video picture.
 *
 * The only `AndroidView` in the rebuilt player. A `TextureView` rather than a `SurfaceView`
 * because the screenshot feature reads frames back, and because the zoom gesture applies a
 * `graphicsLayer` transform that a `SurfaceView`'s independent surface would ignore.
 *
 * Sizing is this composable's job. The legacy player got it from the `AspectRatioFrameLayout`
 * inside `PlayerView`; with `PlayerView` gone nothing else lays the picture out to its real
 * proportions, so it would render stretched to the container.
 *
 * @param videoWidth width of the video as the player reports it, `0` until it is known.
 * @param videoHeight height of the video as the player reports it, `0` until it is known.
 * @param onSurfaceReady the view to render into, once it exists.
 * @param onSurfaceReleased the view is going away and must not be rendered into any more.
 */
@Composable
internal fun VideoSurface(
    videoWidth: Int,
    videoHeight: Int,
    onSurfaceReady: (TextureView) -> Unit,
    onSurfaceReleased: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { context -> TextureView(context).also(onSurfaceReady) },
            onRelease = { onSurfaceReleased() },
            modifier = Modifier
                .testTag(VIDEO_SURFACE_TEST_TAG)
                .videoSize(videoWidth, videoHeight),
        )
    }
}

/**
 * Constrains the picture to the video's own aspect ratio, filling the container until the player
 * reports a size. Either dimension can be `0` before the first frame is decoded, and dividing by
 * it would produce a ratio no layout can satisfy.
 */
private fun Modifier.videoSize(videoWidth: Int, videoHeight: Int): Modifier =
    if (videoWidth > 0 && videoHeight > 0) {
        aspectRatio(videoWidth.toFloat() / videoHeight.toFloat())
    } else {
        fillMaxSize()
    }

/** The video picture itself, tagged so a test can measure what it was laid out to. */
const val VIDEO_SURFACE_TEST_TAG = "video_player:surface"
