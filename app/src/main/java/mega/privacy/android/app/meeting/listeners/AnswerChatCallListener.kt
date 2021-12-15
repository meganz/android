package mega.privacy.android.app.meeting.listeners

import android.content.Context
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.listeners.ChatBaseListener
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.*

class AnswerChatCallListener(context: Context?) : ChatBaseListener(context) {
    private var callback: OnCallAnsweredCallback? = null

    constructor(
        context: Context?,
        callback: OnCallAnsweredCallback
    ) : this(context) {
        this.callback = callback
    }

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type != MegaChatRequest.TYPE_ANSWER_CHAT_CALL) {
            return
        }

        if (MegaApplication.getChatManagement().isAlreadyJoiningCall(request.chatHandle)) {
            MegaApplication.getChatManagement().removeJoiningCallChatId(request.chatHandle)
        }

        if (e.errorCode == MegaError.API_OK) {
            LogUtil.logDebug("Call answered")

            val call: MegaChatCall = api.getChatCall(request.chatHandle)
            MegaApplication.getChatManagement().setRequestSentCall(call.callId, false)
            callback?.onCallAnswered(request.chatHandle, request.flag)
        } else {
            LogUtil.logError("Error answering the call. Error code "+e.errorCode)
            MegaApplication.getInstance().removeRTCAudioManagerRingIn()
            callback?.onErrorAnsweredCall(e.errorCode)
        }
    }

    interface OnCallAnsweredCallback {
        fun onCallAnswered(chatId: Long, flag: Boolean)
        fun onErrorAnsweredCall(errorCode: Int)
    }
}