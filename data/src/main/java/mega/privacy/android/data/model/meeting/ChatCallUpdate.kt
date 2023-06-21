package mega.privacy.android.data.model.meeting

import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatCallListenerInterface
import nz.mega.sdk.MegaChatSession

/**
 * Chat call update events corresponding to [MegaChatCallListenerInterface] callbacks.
 */
sealed class ChatCallUpdate {

    /**
     * On chat call update
     *
     * @property item
     */
    data class OnChatCallUpdate(val item: MegaChatCall?) : ChatCallUpdate()

    /**
     * On chat session update
     *
     * @property chatId
     * @property callId
     * @property session
     */
    data class OnChatSessionUpdate(
        val chatId: Long,
        val callId: Long,
        val session: MegaChatSession?,
    ) : ChatCallUpdate()
}
