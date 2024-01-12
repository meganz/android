package mega.privacy.android.app.meeting.listeners

import android.graphics.Bitmap
import android.view.TextureView
import mega.privacy.android.app.meeting.MegaSurfaceRenderer
import mega.privacy.android.app.utils.Constants.INVALID_DIMENSION
import mega.privacy.android.app.utils.VideoCaptureUtils
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatVideoListenerInterface
import java.nio.ByteBuffer

class GroupVideoListener(
    textureView: TextureView,
    peerId: Long,
    clientId: Long,
    isMe: Boolean,
    isScreenShared: Boolean
) : MegaChatVideoListenerInterface {

    var width = 0
    var height = 0
    private var bitmap: Bitmap? = null
    var textureView: TextureView? = null
    private var isLocal = false
    var localRenderer: MegaSurfaceRenderer? = null

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
            val viewWidth = textureView!!.width
            val viewHeight = textureView!!.height
            if (viewWidth != 0 && viewHeight != 0) {
                bitmap = localRenderer!!.createBitmap(width, height)
            } else {
                this.width = INVALID_DIMENSION
                this.height = INVALID_DIMENSION
            }
        }

        (bitmap ?: return).copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer))

        if (!isLocal || VideoCaptureUtils.isVideoAllowed()) {
            localRenderer!!.drawBitmap(isLocal)
        }
    }

    init {
        this.width = 0
        this.height = 0
        this.textureView = textureView
        this.isLocal = isMe
        this.localRenderer = MegaSurfaceRenderer(textureView, peerId, clientId, isScreenShared)
    }
}