package mega.privacy.android.app.listeners

import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatListenerInterface
import nz.mega.sdk.MegaChatPresenceConfig

/**
 * MegaChatListenerInterface with optional callbacks.
 */
class OptionalMegaChatListenerInterface(
    private val onChatListItemUpdate: ((MegaChatListItem) -> Unit)? = null,
    private val onChatInitStateUpdate: ((Int) -> Unit)? = null,
    private val onChatOnlineStatusUpdate: ((Long, Int, Boolean) -> Unit)? = null,
    private val onChatPresenceConfigUpdate: ((MegaChatPresenceConfig) -> Unit)? = null,
    private val onChatConnectionStateUpdate: ((Long, Int) -> Unit)? = null,
    private val onChatPresenceLastGreen: ((Long, Int) -> Unit)? = null,
    private val onDbError: ((Int, String) -> Unit)? = null
) : MegaChatListenerInterface {

    override fun onChatListItemUpdate(
        api: MegaChatApiJava,
        item: MegaChatListItem
    ) {
        onChatListItemUpdate?.invoke(item)
    }

    override fun onChatInitStateUpdate(
        api: MegaChatApiJava,
        newState: Int
    ) {
        onChatInitStateUpdate?.invoke(newState)
    }

    override fun onChatOnlineStatusUpdate(
        api: MegaChatApiJava,
        userHandle: Long,
        status: Int,
        inProgress: Boolean
    ) {
        onChatOnlineStatusUpdate?.invoke(userHandle, status, inProgress)
    }

    override fun onChatPresenceConfigUpdate(
        api: MegaChatApiJava,
        config: MegaChatPresenceConfig
    ) {
        onChatPresenceConfigUpdate?.invoke(config)
    }

    override fun onChatConnectionStateUpdate(
        api: MegaChatApiJava,
        chatid: Long,
        newState: Int
    ) {
        onChatConnectionStateUpdate?.invoke(chatid, newState)
    }

    override fun onChatPresenceLastGreen(
        api: MegaChatApiJava,
        userHandle: Long,
        lastGreen: Int
    ) {
        onChatPresenceLastGreen?.invoke(userHandle, lastGreen)
    }

    override fun onDbError(
        api: MegaChatApiJava,
        error: Int,
        msg: String
    ) {
        onDbError?.invoke(error, msg)
    }
}
