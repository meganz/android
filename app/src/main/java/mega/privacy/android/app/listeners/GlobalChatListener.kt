package mega.privacy.android.app.listeners

import android.content.Intent
import android.util.Pair
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.EventConstants.EVENT_CHAT_CONNECTION_STATUS
import mega.privacy.android.app.constants.EventConstants.EVENT_CHAT_TITLE_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_PRIVILEGES_CHANGE
import mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_SIGNAL_PRESENCE
import mega.privacy.android.app.utils.Constants.EVENT_CHAT_STATUS_CHANGE
import mega.privacy.android.app.utils.RunOnUIThreadUtils.post
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApi.INIT_ONLINE_SESSION
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatListenerInterface
import nz.mega.sdk.MegaChatPresenceConfig
import nz.mega.sdk.MegaChatRoom
import timber.log.Timber

class GlobalChatListener(private val application: MegaApplication) : MegaChatListenerInterface {
    override fun onChatListItemUpdate(api: MegaChatApiJava?, item: MegaChatListItem?) {
        if (item != null) {
            if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_OWN_PRIV) || item.hasChanged(
                    MegaChatListItem.CHANGE_TYPE_PARTICIPANTS
                )
            ) {
                LiveEventBus.get(
                    EVENT_PRIVILEGES_CHANGE,
                    MegaChatListItem::class.java
                ).post(item)
            }

            if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_TITLE)) {
                api?.let {
                    it.getChatRoom(item.chatId)?.let { chat ->
                        LiveEventBus.get(
                            EVENT_CHAT_TITLE_CHANGE,
                            MegaChatRoom::class.java
                        ).post(chat)
                    }
                }
            }

            application.onChatListItemUpdate(api, item)
        }
    }

    override fun onChatInitStateUpdate(api: MegaChatApiJava?, newState: Int) {
        if (newState == INIT_ONLINE_SESSION) {
            api?.let {
                val list = it.chatListItems
                if (!list.isNullOrEmpty()) {
                    for (i in 0 until list.size) {
                        MegaApplication.getChatManagement().addCurrentGroupChat(list[i].chatId)
                    }
                }
            }
        }
    }

    override fun onChatOnlineStatusUpdate(
        api: MegaChatApiJava?,
        userhandle: Long,
        status: Int,
        inProgress: Boolean,
    ) {
        if (userhandle == api?.myUserHandle) {
            LiveEventBus.get(EVENT_CHAT_STATUS_CHANGE, Int::class.java).post(status)
        }
    }

    override fun onChatPresenceConfigUpdate(
        api: MegaChatApiJava?,
        config: MegaChatPresenceConfig?,
    ) {
        if (config?.isPending == false) {
            Timber.d("Launch local broadcast")
            val intent = Intent(BROADCAST_ACTION_INTENT_SIGNAL_PRESENCE)
            application.sendBroadcast(intent)
        }
    }

    override fun onChatConnectionStateUpdate(api: MegaChatApiJava?, chatid: Long, newState: Int) {
        val chatRoom = api!!.getChatRoom(chatid)
        if (newState == MegaChatApi.CHAT_CONNECTION_ONLINE && chatRoom != null) {
            val chatIdAndState = Pair.create(chatid, newState)
            LiveEventBus.get(
                EVENT_CHAT_CONNECTION_STATUS,
                Pair::class.java
            ).post(chatIdAndState)
        }
    }

    override fun onChatPresenceLastGreen(api: MegaChatApiJava?, userhandle: Long, lastGreen: Int) {
    }

    override fun onDbError(api: MegaChatApiJava?, error: Int, msg: String?) {
        Timber.e("MEGAChatSDK onDBError occurred. Error $error with message $msg")
        when (error) {
            MegaChatApi.DB_ERROR_IO -> application.currentActivity?.finishAndRemoveTask()

            MegaChatApi.DB_ERROR_FULL -> post {
                application.currentActivity?.let {
                    Util.showErrorAlertDialog(
                        StringResourcesUtils.getString(R.string.error_not_enough_free_space),
                        true, it
                    )
                }
            }
        }
    }
}
