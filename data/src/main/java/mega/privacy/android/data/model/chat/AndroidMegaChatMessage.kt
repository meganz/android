package mega.privacy.android.data.model.chat

import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.contacts.ContactLink
import nz.mega.sdk.MegaChatMessage

/**
 * Android mega chat message
 *
 * @property message
 * @property pendingMessage
 * @property infoToShow
 * @property isShowAvatar
 * @property isUploading
 * @property richLinkMessage
 * @property contactLinkResult
 */
data class AndroidMegaChatMessage(
    var message: MegaChatMessage? = null,
    var pendingMessage: PendingMessage? = null,
    var infoToShow: Int = -1,
    var isShowAvatar: Boolean = true,
    var isUploading: Boolean = false,
    var richLinkMessage: AndroidMegaRichLinkMessage? = null,
    var contactLinkResult: ContactLink? = null,
) {

    constructor(message: MegaChatMessage?) : this() {
        this.message = message
    }

    constructor(pendingMessage: PendingMessage?, uploading: Boolean) : this(
        pendingMessage = pendingMessage,
        isUploading = uploading
    )

    companion object {
        const val CHAT_ADAPTER_SHOW_TIME = 1
        const val CHAT_ADAPTER_SHOW_NOTHING = 0
        const val CHAT_ADAPTER_SHOW_ALL = 2
    }
}