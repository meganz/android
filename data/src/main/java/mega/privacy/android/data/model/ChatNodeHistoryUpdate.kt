package mega.privacy.android.data.model

import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatNodeHistoryListenerInterface

/**
 * Chat node history events corresponding to [MegaChatNodeHistoryListenerInterface] callbacks.
 */
sealed interface ChatNodeHistoryUpdate {

    /**
     * The node history is open, so attachments can be loaded.
     */
    data object Opened : ChatNodeHistoryUpdate

    /**
     * An attachment message loaded after a request to load attachments.
     *
     * @property msg The loaded [MegaChatMessage], or null once the requested batch has been
     * delivered or no more history is available.
     */
    data class Loaded(val msg: MegaChatMessage?) : ChatNodeHistoryUpdate

    /**
     * A new attachment message received in the chat.
     *
     * @property msg The received [MegaChatMessage].
     */
    data class Received(val msg: MegaChatMessage) : ChatNodeHistoryUpdate

    /**
     * An attachment message deleted from the chat.
     *
     * @property msgId Id of the deleted message.
     */
    data class Deleted(val msgId: Long) : ChatNodeHistoryUpdate

    /**
     * The node history truncated.
     *
     * @property msgId Id of the message from which the history has been truncated, or
     * MEGACHAT_INVALID_HANDLE if no messages are left.
     */
    data class Truncated(val msgId: Long) : ChatNodeHistoryUpdate
}
