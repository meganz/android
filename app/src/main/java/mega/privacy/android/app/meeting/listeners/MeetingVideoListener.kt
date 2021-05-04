package mega.privacy.android.app.meeting.listeners

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.util.DisplayMetrics
import android.view.SurfaceHolder
import android.view.SurfaceView
import mega.privacy.android.app.lollipop.megachat.calls.MegaSurfaceRenderer
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.VideoCaptureUtils
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatVideoListenerInterface
import java.nio.ByteBuffer

/**
 * A listener for metadata corresponding to video being rendered.
 */
class MeetingVideoListener(
    private val surfaceView: SurfaceView,
    outMetrics: DisplayMetrics?,
    clientId: Long,
    isFloatingWindow: Boolean = true
) : MegaChatVideoListenerInterface {

    var width = 0
    var height = 0
    private var isLocal = true
    val renderer: MegaSurfaceRenderer
    private val surfaceHolder: SurfaceHolder
    private var bitmap: Bitmap? = null
    private var viewWidth = 0
    private var viewHeight = 0

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

        /**
         * viewWidth != surfaceView.width || viewHeight != surfaceView.height
         * Re-calculate the camera preview ratio when surface view size changed
         */

        if (this.width != width || this.height != height
            || viewWidth != surfaceView.width || viewHeight != surfaceView.height) {
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
        isLocal = clientId == MEGACHAT_INVALID_HANDLE

        if (isFloatingWindow && isLocal) {
            this.surfaceView.setZOrderMediaOverlay(true);
        } else if (!isFloatingWindow) {
            if (!isLocal) {
                this.surfaceView.setZOrderOnTop(false);
            }
        }

        surfaceHolder = surfaceView.holder
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT)
        renderer = MegaSurfaceRenderer(surfaceView, isFloatingWindow, outMetrics)
    }
}