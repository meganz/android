package mega.privacy.android.app.listeners

import android.content.Intent
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.fragments.homepage.notifyChatOnlineStatusChange
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatListenerInterface
import nz.mega.sdk.MegaChatPresenceConfig

class GlobalChatListener(private val application: MegaApplication) : MegaChatListenerInterface {
    override fun onChatListItemUpdate(api: MegaChatApiJava?, item: MegaChatListItem?) {
        application.onChatListItemUpdate(api, item)
    }

    override fun onChatInitStateUpdate(api: MegaChatApiJava?, newState: Int) {
    }

    override fun onChatOnlineStatusUpdate(
        api: MegaChatApiJava?,
        userhandle: Long,
        status: Int,
        inProgress: Boolean
    ) {
        if (userhandle == api?.myUserHandle) {
            notifyChatOnlineStatusChange(status)
        }
    }

    override fun onChatPresenceConfigUpdate(
        api: MegaChatApiJava?,
        config: MegaChatPresenceConfig?
    ) {
        if (config?.isPending == false) {
            LogUtil.logDebug("Launch local broadcast")
            val intent = Intent(Constants.BROADCAST_ACTION_INTENT_SIGNAL_PRESENCE)
            application.sendBroadcast(intent)
        }
    }

    override fun onChatConnectionStateUpdate(api: MegaChatApiJava?, chatid: Long, newState: Int) {
    }

    override fun onChatPresenceLastGreen(api: MegaChatApiJava?, userhandle: Long, lastGreen: Int) {
    }
}
