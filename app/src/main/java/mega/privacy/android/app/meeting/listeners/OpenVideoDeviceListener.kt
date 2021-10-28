package mega.privacy.android.app.meeting.listeners

import android.content.Context
import mega.privacy.android.app.listeners.ChatBaseListener
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.*

class OpenVideoDeviceListener(context: Context?) : ChatBaseListener(context) {

    private var callback: OnOpenVideoDeviceCallback? = null

    constructor(
        context: Context?,
        callback: OnOpenVideoDeviceCallback
    ) : this(context) {
        this.callback = callback
    }
    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type != MegaChatRequest.TYPE_OPEN_VIDEO_DEVICE) {
            return
        }

        if (e.errorCode == MegaError.API_OK) {
            val isEnabled = request.flag
            when {
                isEnabled -> LogUtil.logDebug("Video opened")
                else -> LogUtil.logDebug("Video closed")
            }
            callback?.onVideoDeviceOpened(isEnabled)
        } else {
            LogUtil.logError("Error Opened Video Device. Error code "+e.errorCode)
        }
    }

    interface OnOpenVideoDeviceCallback {
        fun onVideoDeviceOpened(isVideoOn: Boolean)
    }
}