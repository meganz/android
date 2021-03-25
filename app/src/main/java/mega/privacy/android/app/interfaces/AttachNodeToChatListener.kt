package mega.privacy.android.app.interfaces

import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage

interface AttachNodeToChatListener {
    fun onSendSuccess(message: AndroidMegaChatMessage)
}
