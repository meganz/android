package mega.privacy.android.app.meeting.listeners

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.util.DisplayMetrics
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.github.barteksc.pdfviewer.util.MathUtils.min
import mega.privacy.android.app.meeting.MegaSurfaceRenderer
import mega.privacy.android.app.utils.Constants.INVALID_DIMENSION
import mega.privacy.android.app.utils.VideoCaptureUtils
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatVideoListenerInterface
import java.nio.ByteBuffer

/**
 * A listener for metadata corresponding to video being rendered.
 */
class IndividualCallVideoListener(
    private val surfaceView: SurfaceView,
    outMetrics: DisplayMetrics?,
    clientId: Long,
    isFloatingWindow: Boolean = true,
    isOneToOneCall: Boolean = true
) : MegaChatVideoListenerInterface {

    var width = 0
    var height = 0
    var isFloatingWindow = false
    private var isLocal = true
    val renderer: MegaSurfaceRenderer
    private val surfaceHolder: SurfaceHolder
    private var bitmap: Bitmap? = null
    private var viewWidth = 0
    private var viewHeight = 0

    fun setAlpha(alpha: Int) {
        renderer.setAlpha(alpha)
    }

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

        // viewWidth != surfaceView.width || viewHeight != surfaceView.height
        // Re-calculate the camera preview ratio when surface view size changed
        if (this.width != width || this.height != height
            || viewWidth != surfaceView.width || viewHeight != surfaceView.height) {
            this.width = width
            this.height = height
            val holder = surfaceView.holder

            if(holder != null){
                val viewWidth = surfaceView.width
                val viewHeight = surfaceView.height

                if (viewWidth != 0 && viewHeight != 0) {
                    var holderWidth = min(viewWidth, width)
                    var holderHeight = holderWidth * viewHeight / viewWidth

                    if (holderHeight > viewHeight) {
                        holderHeight = viewHeight
                        holderWidth = holderHeight * viewWidth / viewHeight
                    }

                    bitmap = renderer.createBitmap(width, height)

                    if(!isLocal){
                        holder.setFixedSize(holderWidth, holderHeight)
                    }

                } else {
                    this.width = INVALID_DIMENSION
                    this.height = INVALID_DIMENSION
                }
            }
        }

        if (bitmap == null) return

        bitmap!!.copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer))
        if (VideoCaptureUtils.isVideoAllowed()) {
            renderer.drawBitmap(isLocal, false)
        }
    }

    init {
        isLocal = clientId == MEGACHAT_INVALID_HANDLE

        this.isFloatingWindow = isFloatingWindow

        if ((isFloatingWindow && isLocal) || !isOneToOneCall) {
            this.surfaceView.setZOrderOnTop(true)
        } else if (!isFloatingWindow && !isLocal) {
            this.surfaceView.setZOrderOnTop(false)
        }

        surfaceHolder = surfaceView.holder
        surfaceHolder.setFormat(PixelFormat.TRANSLUCENT)
        renderer = MegaSurfaceRenderer(
            surfaceView,
            isFloatingWindow,
            outMetrics
        )
    }
}