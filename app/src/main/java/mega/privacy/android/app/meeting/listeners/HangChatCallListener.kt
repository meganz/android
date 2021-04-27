package mega.privacy.android.app.meeting.listeners

import android.content.Context
import mega.privacy.android.app.listeners.ChatBaseListener
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.*

class HangChatCallListener(context: Context?) : ChatBaseListener(context) {

    private var callback: OnCallHungUpCallback? = null

    constructor(
        context: Context?,
        callback: OnCallHungUpCallback
    ) : this(context) {
        this.callback = callback
    }

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type != MegaChatRequest.TYPE_HANG_CHAT_CALL) {
            return
        }

        if (e.errorCode == MegaError.API_OK) {
            logDebug("Call hung up")
            callback?.onCallHungUp(request.chatHandle)
        } else {
            LogUtil.logError("Error Hanging up the call")
        }
    }

    interface OnCallHungUpCallback {
        fun onCallHungUp(callId: Long)
    }
}