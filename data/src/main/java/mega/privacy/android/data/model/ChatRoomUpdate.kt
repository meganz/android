package mega.privacy.android.data.model

import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatRoomListenerInterface

/**
 * Chat Room update events corresponding to [MegaChatRoomListenerInterface] callbacks.
 */
sealed class ChatRoomUpdate {
    /**
     * Changes in the chatroom.
     *
     * @property chat [MegaChatRoom] that contains the updates relatives to the chat.
     */
    data class OnChatRoomUpdate(val chat: MegaChatRoom?) : ChatRoomUpdate()

    /**
     * On message loaded update.
     *
     * @property msg [MegaChatMessage] that contains the msg.
     */
    data class OnMessageLoaded(val msg: MegaChatMessage?) : ChatRoomUpdate()

    /**
     * On message received update.
     *
     * @property msg [MegaChatMessage] representing the received message.
     */
    data class OnMessageReceived(val msg: MegaChatMessage?) : ChatRoomUpdate()

    /**
     * On message update.
     *
     * @property msg [MegaChatMessage] representing the updated message.
     */
    data class OnMessageUpdate(val msg: MegaChatMessage?) : ChatRoomUpdate()

    /**
     * On history reloaded update.
     *
     * @property chat [MegaChatRoom] whose local history is about to be discarded.
     */
    data class OnHistoryReloaded(val chat: MegaChatRoom?) : ChatRoomUpdate()

    /**
     * On reaction update.
     *
     * @property msgId The message id.
     * @property reaction  String that represents the reaction.
     * @property count  Number of users who have reacted to this message with the same reaction.
     */
    data class OnReactionUpdate(val msgId: Long, val reaction: String, val count: Int) :
        ChatRoomUpdate()

    /**
     * On history truncated by retention time update.
     *
     * @property msg [MegaChatMessage] whose timestamp has exceeded retention time
     */
    data class OnHistoryTruncatedByRetentionTime(val msg: MegaChatMessage) : ChatRoomUpdate()
}