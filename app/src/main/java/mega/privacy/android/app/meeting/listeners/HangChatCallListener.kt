package mega.privacy.android.app.meeting.listeners

import android.content.Context
import mega.privacy.android.app.listeners.ChatBaseListener
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError
import timber.log.Timber

class HangChatCallListener(context: Context?) : ChatBaseListener(context) {

    private var callback: OnCallHungUpCallback? = null

    constructor(
        context: Context?,
        callback: OnCallHungUpCallback,
    ) : this(context) {
        this.callback = callback
    }

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type != MegaChatRequest.TYPE_HANG_CHAT_CALL) {
            return
        }

        if (e.errorCode == MegaError.API_OK) {
            Timber.d("Call hung up")
            callback?.onCallHungUp(request.chatHandle)
        } else {
            Timber.e("Error Hanging up the call. Error code ${e.errorCode}")
        }
    }

    interface OnCallHungUpCallback {
        fun onCallHungUp(callId: Long)
    }
}