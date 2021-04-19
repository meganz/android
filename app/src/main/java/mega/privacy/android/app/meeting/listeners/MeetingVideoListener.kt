package mega.privacy.android.app.meeting.listeners

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.util.DisplayMetrics
import android.view.SurfaceHolder
import android.view.SurfaceView
import mega.privacy.android.app.lollipop.megachat.calls.MegaSurfaceRenderer
import mega.privacy.android.app.utils.VideoCaptureUtils
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatVideoListenerInterface
import java.nio.ByteBuffer

/**
 * A listener for metadata corresponding to video being rendered.
 */
class MeetingVideoListener(
    context: Context?,
    private val surfaceView: SurfaceView,
    outMetrics: DisplayMetrics?,
    isFrontCamera: Boolean = true
) : MegaChatVideoListenerInterface {
    var width = 0
    var height = 0
    private val isLocal = true
    val renderer: MegaSurfaceRenderer
    private val surfaceHolder: SurfaceHolder
    private var bitmap: Bitmap? = null
    override fun onChatVideoData(
        api: MegaChatApiJava,
        chatid: Long,
        width: Int,
        height: Int,
        byteBuffer: ByteArray
    ) {
        if (width == 0 || height == 0) {
            return
        }
        if (this.width != width || this.height != height) {
            this.width = width
            this.height = height
            val holder = surfaceView.holder
            if (holder != null) {
                val viewWidth = surfaceView.width
                val viewHeight = surfaceView.height
                if (viewWidth != 0 && viewHeight != 0) {
                    var holderWidth = Math.min(viewWidth, width)
                    var holderHeight = holderWidth * viewHeight / viewWidth
                    if (holderHeight > viewHeight) {
                        holderHeight = viewHeight
                        holderWidth = holderHeight * viewWidth / viewHeight
                    }
                    bitmap = renderer.createBitmap(width, height)
                    holder.setFixedSize(holderWidth, holderHeight)
                } else {
                    this.width = -1
                    this.height = -1
                }
            }
        }
        if (bitmap == null) return
        bitmap!!.copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer))
        if (VideoCaptureUtils.isVideoAllowed()) {
            renderer.drawBitmap(isLocal)
        }
    }

    init {
        if (isFrontCamera && isLocal) {
            surfaceView.setZOrderMediaOverlay(true)
        }
        surfaceHolder = surfaceView.holder
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT)
        renderer = MegaSurfaceRenderer(surfaceView, isFrontCamera, outMetrics)
    }
}