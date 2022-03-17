package mega.privacy.android.app.listeners

import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.MegaChatApi.CHAT_CONNECTION_ONLINE
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatListenerInterface
import nz.mega.sdk.MegaChatPresenceConfig

/**
 * Temp listener for meeting's "Join as guest".
 * Once the connection is successful, it will be removed from chatApi.
 */
class ChatConnectionListener(
    private val targetChatId: Long,
    private val callback: () -> Unit
) : MegaChatListenerInterface {

    override fun onChatListItemUpdate(api: MegaChatApiJava?, item: MegaChatListItem?) {

    }

    override fun onChatInitStateUpdate(api: MegaChatApiJava?, newState: Int) {

    }

    override fun onChatOnlineStatusUpdate(
        api: MegaChatApiJava?,
        userhandle: Long,
        status: Int,
        inProgress: Boolean
    ) {

    }

    override fun onChatPresenceConfigUpdate(
        api: MegaChatApiJava?,
        config: MegaChatPresenceConfig?
    ) {

    }

    override fun onChatConnectionStateUpdate(api: MegaChatApiJava?, chatid: Long, newState: Int) {
        if(chatid == targetChatId && newState == CHAT_CONNECTION_ONLINE) {
            LogUtil.logDebug("Connect to chat $chatid successfully!")
            callback()
            api?.removeChatListener(this)
        }
    }

    override fun onChatPresenceLastGreen(api: MegaChatApiJava?, userhandle: Long, lastGreen: Int) {

    }

    override fun onDbError(api: MegaChatApiJava?, error: Int, msg: String?) {
    }
}