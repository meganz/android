package mega.privacy.android.app.listeners

import android.content.Context
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError
import timber.log.Timber

class EditChatRoomNameListener(context: Context?) : ChatBaseListener(context) {
    private var callback: OnEditedChatRoomNameCallback? = null

    constructor(
        context: Context?,
        callback: OnEditedChatRoomNameCallback,
    ) : this(context) {
        this.callback = callback
    }

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type != MegaChatRequest.TYPE_EDIT_CHATROOM_NAME) {
            return
        }

        when (e.errorCode) {
            MegaError.API_OK -> {
                Timber.d("ChatRoom name edited")
                callback?.onEditedChatRoomName(request.chatHandle, request.text)
            }
            else -> {
                Timber.e("Error Editing ChatRoom name")
            }
        }
    }

    interface OnEditedChatRoomNameCallback {
        fun onEditedChatRoomName(chatId: Long, name: String)
    }
}