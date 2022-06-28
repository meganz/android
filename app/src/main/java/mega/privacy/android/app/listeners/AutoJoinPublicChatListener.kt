package mega.privacy.android.app.listeners

import android.content.Context
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError
import timber.log.Timber

class AutoJoinPublicChatListener(context: Context?) : ChatBaseListener(context) {

    private var callback: OnJoinedChatCallback? = null

    constructor(
        context: Context?,
        callback: OnJoinedChatCallback,
    ) : this(context) {
        this.callback = callback
    }

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type != MegaChatRequest.TYPE_AUTOJOIN_PUBLIC_CHAT) {
            return
        }

        if (e.errorCode == MegaError.API_OK) {
            Timber.d("Joined chat correctly")
            callback?.onJoinedChat(request.chatHandle, request.userHandle)
        } else {
            Timber.e("Error Joining the chat, e.errorCode ${e.errorCode}")
            callback?.onErrorJoinedChat(request.chatHandle, request.userHandle, e.errorCode)
        }
    }

    interface OnJoinedChatCallback {
        fun onJoinedChat(chatId: Long, userHandle: Long)
        fun onErrorJoinedChat(chatId: Long, userHandle: Long, error: Int)
    }
}