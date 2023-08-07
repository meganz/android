package mega.privacy.android.app.interfaces

import mega.privacy.android.data.model.chat.AndroidMegaChatMessage

interface AttachNodeToChatListener {
    fun onSendSuccess(message: AndroidMegaChatMessage)
}
