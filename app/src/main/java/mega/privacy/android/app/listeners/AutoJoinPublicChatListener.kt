package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError

class AutoJoinPublicChatListener(context: Context?) : ChatBaseListener(context) {

    private var callback: OnJoinedChatCallback? = null

    constructor(
        context: Context?,
        callback: OnJoinedChatCallback
    ) : this(context) {
        this.callback = callback
    }

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type != MegaChatRequest.TYPE_AUTOJOIN_PUBLIC_CHAT) {
            return
        }

        if (e.errorCode == MegaError.API_OK) {
            LogUtil.logDebug("Joined chat correctly")
            callback?.onJoinedChat(request.chatHandle, request.userHandle)
        } else {
            LogUtil.logError("Error Joining the chat, e.errorCode " + e.errorCode)
            callback?.onErrorJoinedChat(request.chatHandle, request.userHandle, e.errorCode)
        }
    }

    interface OnJoinedChatCallback {
        fun onJoinedChat(chatId: Long, userHandle: Long)
        fun onErrorJoinedChat(chatId: Long, userHandle: Long, error:Int)
    }
}