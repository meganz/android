package mega.privacy.android.app.meeting.listeners

import android.graphics.Bitmap
import android.util.DisplayMetrics
import android.view.TextureView
import mega.privacy.android.app.meeting.MegaSurfaceRenderer
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.VideoCaptureUtils
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatVideoListenerInterface
import java.nio.ByteBuffer

/**
 * A listener for metadata corresponding to video being rendered.
 */
class IndividualCallVideoListener(
    private val textureView: TextureView,
    outMetrics: DisplayMetrics?,
    clientId: Long,
    isFloatingWindow: Boolean = true
) : MegaChatVideoListenerInterface {

    var width = 0
    var height = 0
    private var isFloatingWindow = false
    private var isLocal = true
    val renderer: MegaSurfaceRenderer
    private var bitmap: Bitmap? = null

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

        if (this.width != width || this.height != height) {
            this.width = width
            this.height = height
            val viewWidth = textureView.width
            val viewHeight = textureView.height
            if (viewWidth != 0 && viewHeight != 0) {
                bitmap = renderer.createBitmap(width, height)
            } else {
                this.width = Constants.INVALID_DIMENSION
                this.height = Constants.INVALID_DIMENSION
            }
        }

        (bitmap ?: return).copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer))
        if (VideoCaptureUtils.isVideoAllowed()) {
            renderer.drawBitmap(isLocal)
        }
    }

    init {
        isLocal = clientId == MEGACHAT_INVALID_HANDLE

        this.isFloatingWindow = isFloatingWindow

        renderer = MegaSurfaceRenderer(
            textureView,
            isFloatingWindow,
            outMetrics
        )
    }
}