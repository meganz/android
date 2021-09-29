package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.*

class EditChatRoomNameListener(context: Context?) : ChatBaseListener(context) {
    private var callback: OnEditedChatRoomNameCallback? = null

    constructor(
        context: Context?,
        callback: OnEditedChatRoomNameCallback
    ) : this(context) {
        this.callback = callback
    }

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type != MegaChatRequest.TYPE_EDIT_CHATROOM_NAME) {
            return
        }

        when (e.errorCode) {
            MegaError.API_OK -> {
                LogUtil.logDebug("ChatRoom name edited")
                callback?.onEditedChatRoomName(request.chatHandle, request.text)
            }
            else -> {
                LogUtil.logError("Error Editing ChatRoom name")
            }
        }
    }

    interface OnEditedChatRoomNameCallback {
        fun onEditedChatRoomName(chatId: Long, name: String)
    }
}