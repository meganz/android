package mega.privacy.android.app.components

import android.content.Context
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaPushNotificationSettings
import nz.mega.sdk.MegaPushNotificationSettingsAndroid
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton class holding data and operations related to [MegaPushNotificationSettings]
 *
 * This class is deprecated and will be removed in the future once all operations are moved to
 * [mega.privacy.android.domain.repository.NotificationsRepository]
 */
@Singleton
class PushNotificationSettingManagement @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
) {
    private var push: MegaPushNotificationSettings? = null

    /**
     * Method for getting the PushNotificationSetting instance.
     *
     * @return MegaPushNotificationSettings.
     */
    val pushNotificationSetting: MegaPushNotificationSettings?
        get() = push

    init {
        updateMegaPushNotificationSetting()
    }


    /**
     * Method for getting MegaPushNotificationSettings from megaApi.
     *
     * The result will be received in [mega.privacy.android.app.globalmanagement.BackgroundRequestListener]
     * and then set in function [setPushNotificationSettings]
     */
    fun updateMegaPushNotificationSetting() {
        megaApi.getPushNotificationSettings(null)
    }

    /**
     * Set the push notification settings
     *
     * @param receivedPush The MegaPushNotificationSettings obtained from the request.
     */
    fun setPushNotificationSettings(receivedPush: MegaPushNotificationSettings?) {
        push = receivedPush?.let {
            MegaPushNotificationSettingsAndroid.copy(it)
        } ?: MegaPushNotificationSettings.createInstance()
    }

    /**
     * Method that controls the change in the notifications of a specific chat.
     *
     * @param context Context of Activity.
     * @param option  Muting option selected.
     * @param chatId  Chat ID.
     */
    fun controlMuteNotificationsOfAChat(context: Context?, option: String?, chatId: Long) {
        megaChatApi.getChatListItem(chatId)?.let { chat ->
            controlMuteNotifications(context, option, listOf(chat))
        }
    }

    /**
     * Method that controls the change in general and specific chat notifications.
     *
     * @param context Context of Activity.
     * @param option  Muting option selected
     * @param chats   List of Chats.
     */
    fun controlMuteNotifications(
        context: Context?,
        option: String?,
        chats: List<MegaChatListItem>?
    ) {
        when (option) {
            Constants.NOTIFICATIONS_DISABLED -> {
                chats?.forEach { chat ->
                    push?.enableChat(chat.chatId, false)
                } ?: run {
                    push?.enableChats(false)
                }
            }

            Constants.NOTIFICATIONS_ENABLED ->
                chats?.forEach { chat ->
                    push?.enableChat(chat.chatId, true)
                } ?: run {
                    push?.enableChats(true)
                }

            Constants.NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING, Constants.NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING -> {
                val timestamp = TimeUtils.getCalendarSpecificTime(option).timeInMillis / 1000
                chats?.forEach { chat ->
                    push?.setChatDnd(chat.chatId, timestamp)
                } ?: run {
                    push?.globalChatsDnd = timestamp
                }
            }

            else -> {
                val time = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    when (option) {
                        Constants.NOTIFICATIONS_30_MINUTES -> add(Calendar.MINUTE, 30)
                        Constants.NOTIFICATIONS_1_HOUR -> add(Calendar.HOUR, 1)
                        Constants.NOTIFICATIONS_6_HOURS -> add(Calendar.HOUR, 6)
                        Constants.NOTIFICATIONS_24_HOURS -> add(Calendar.HOUR, 24)
                    }
                }.timeInMillis / 1000

                chats?.forEach { chat ->
                    push?.setChatDnd(chat.chatId, time)
                } ?: run {
                    push?.globalChatsDnd = time
                }
            }
        }
        megaApi.setPushNotificationSettings(push, null)
        ChatUtil.muteChat(context, option)
    }
}
