package mega.privacy.android.data.model

import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatListenerInterface
import nz.mega.sdk.MegaChatPresenceConfig

/**
 * Chat update events corresponding to [MegaChatListenerInterface] callbacks.
 */
sealed class ChatUpdate {

    /**
     * On chat list item update.
     *
     * @property item [MegaChatListItem] representing a 1to1 chat.
     */
    data class OnChatListItemUpdate(val item: MegaChatListItem?) : ChatUpdate()

    /**
     * On chat init state update.
     *
     * @property newState New state of initialization.
     */
    data class OnChatInitStateUpdate(val newState: Int) : ChatUpdate()

    /**
     * On chat online status update.
     *
     * @property userHandle User handle of who the status has changed.
     * @property status     New status.
     * @property inProgress Whether the reported status is being set or it is definitive (only for your own changes).
     */
    data class OnChatOnlineStatusUpdate(
        val userHandle: Long,
        val status: Int,
        val inProgress: Boolean,
    ) : ChatUpdate()

    /**
     * On chat presence configuration update.
     *
     * @property config New presence configuration.
     */
    data class OnChatPresenceConfigUpdate(val config: MegaChatPresenceConfig?) : ChatUpdate()

    /**
     * On chat connection state update.
     *
     * @property chatId   Chat handle.
     * @property newState New state of the connection.
     */
    data class OnChatConnectionStateUpdate(val chatId: Long, val newState: Int) : ChatUpdate()

    /**
     * On chat presence last green update.
     *
     * @property userHandle User handle of who the last green has changed.
     * @property lastGreen  Time elapsed (minutes) since the last time user was green.
     */
    data class OnChatPresenceLastGreen(val userHandle: Long, val lastGreen: Int) : ChatUpdate()

    /**
     * On data base error.
     *
     * @property error Error code.
     * @property msg   Error message.
     */
    data class OnDbError(val error: Int, val msg: String?) : ChatUpdate()
}