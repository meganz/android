package mega.privacy.android.core.ui.controls.video

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.util.Log
import android.util.Size
import android.view.TextureView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.withScale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.utils.ComposableLifecycle
import java.nio.ByteBuffer

/**
 * A Composable function that creates a custom [TextureView] to display SDK video streams.
 *
 * This view adapts to the specified size and maintains a CenterCrop scale.
 * It creates a Bitmap from the provided byte array and draws it on the TextureView's canvas.
 * The destination rectangle for the Bitmap is computed based on the aspect ratio of the Bitmap.
 * Resources are cleaned up when the Composable is destroyed.
 *
 * @param videoStream       [Flow] emitting pairs of [Size] and [ByteArray] representing each video frame.
 * @param modifier          [Modifier] to be applied to the TextureView.
 * @param mirrorEffect      Flag to enable/disable image mirror effect.
 */
@Composable
fun MegaVideoTextureView(
    videoStream: Flow<Pair<Size, ByteArray>>,
    modifier: Modifier = Modifier,
    mirrorEffect: Boolean = false,
) {
    val lifecycle = LocalLifecycleOwner.current
    var textureView by remember { mutableStateOf<TextureView?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var bitmapRect by remember { mutableStateOf<Rect?>(null) }
    var frameWidth by remember { mutableStateOf(0) }
    var frameHeight by remember { mutableStateOf(0) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextureView(context).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int,
                    ) {
                        textureView = this@apply
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int,
                    ) {
                        textureView = this@apply
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        textureView = null
                        bitmapRect = null
                        bitmap?.recycle()
                        bitmap = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        },
    )

    // Collects frames from videoStream, updates bitmap, and draws it on TextureView's canvas.
    LaunchedEffect(videoStream) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            videoStream.collectLatest { videoFrame ->
                // Update frame dimensions from the latest video stream
                frameWidth = videoFrame.first.width
                frameHeight = videoFrame.first.height
                if (textureView == null || bitmapRect == null) return@collectLatest

                withContext(Dispatchers.Default) {
                    // Check if existing bitmap has the same dimensions as the current frame
                    if (bitmap?.width != frameWidth || bitmap?.height != frameHeight) {
                        // Recycle existing bitmap and create a new one
                        bitmap?.recycle()
                        bitmap = Bitmap.createBitmap(
                            frameWidth,
                            frameHeight,
                            Bitmap.Config.ARGB_8888
                        )
                    }

                    // Copy pixel data from the byte array to the bitmap
                    bitmap?.copyPixelsFromBuffer(ByteBuffer.wrap(videoFrame.second))
                }

                // Draw the bitmap on the TextureView's canvas
                textureView?.lockCanvas()?.let { canvas ->
                    try {
                        if (mirrorEffect) {
                            // Flip the canvas horizontally for mirror effect
                            canvas.withScale(-1.0f, 1.0f, canvas.width / 2f, canvas.height / 2f) {
                                drawBitmap(bitmap!!, null, bitmapRect!!, null)
                            }
                        } else {
                            canvas.drawBitmap(bitmap!!, null, bitmapRect!!, null)
                        }
                    } catch (error: Exception) {
                        Log.e("TextureView", "Canvas error: ${error.stackTraceToString()}")
                    } finally {
                        textureView?.unlockCanvasAndPost(canvas)
                    }
                }
            }
        }
    }

    // Compute destination rectangle when TextureView or frame dimensions change.
    LaunchedEffect(textureView?.width, textureView?.height, frameWidth, frameHeight) {
        withContext(Dispatchers.Default) {
            bitmapRect = textureView?.computeBitmapRect(frameWidth, frameHeight)
        }
    }

    // Clean up resources when the Composable is destroyed.
    ComposableLifecycle { event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            textureView = null
            bitmapRect = null
            bitmap?.recycle()
            bitmap = null
        }
    }
}

/**
 * Computes the destination rectangle for the Bitmap to be drawn on the TextureView.
 * The dimensions of the rectangle are determined based on the aspect ratios
 * of the Bitmap and the TextureView.
 *
 * @param bitmapWidth   Width of the Bitmap.
 * @param bitmapHeight  Height of the Bitmap.
 * @return              [Rect] representing the destination rectangle for the Bitmap.
 */
private fun TextureView.computeBitmapRect(bitmapWidth: Int, bitmapHeight: Int): Rect {
    val aspectRatioBitmap = bitmapWidth.toFloat() / bitmapHeight.toFloat()
    val aspectRatioView = width.toFloat() / height.toFloat()

    val dstWidth: Int
    val dstHeight: Int

    // Determine the dimensions of the destination rectangle
    if (aspectRatioBitmap > aspectRatioView) {
        // Image is wider relative to the view
        dstHeight = height
        dstWidth = (dstHeight * aspectRatioBitmap).toInt()
    } else {
        // Image is taller relative to the view
        dstWidth = width
        dstHeight = (dstWidth / aspectRatioBitmap).toInt()
    }

    // Calculate any necessary translation to center the image
    val left = (width - dstWidth) / 2
    val top = (height - dstHeight) / 2
    val right = left + dstWidth
    val bottom = top + dstHeight

    return Rect(left, top, right, bottom)
}

@Preview(showBackground = true)
@Composable
internal fun PreviewMegaVideoTextureView() {
    AndroidTheme(isDark = true) {
        MegaVideoTextureView(
            modifier = Modifier.fillMaxSize(),
            videoStream = flowOf(
                Size(100, 100) to ByteArray(100 * 100 * 4) { 255.toByte() }
            )
        )
    }
}
