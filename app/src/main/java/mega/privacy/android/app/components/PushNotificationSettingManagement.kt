package mega.privacy.android.app.components

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.data.repository.LegacyNotificationRepository
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.repository.NotificationsRepository
import nz.mega.sdk.MegaPushNotificationSettings
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
    private val notificationsRepository: NotificationsRepository,
    private val legacyNotificationRepository: LegacyNotificationRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    /**
     * Method for getting the PushNotificationSetting instance.
     *
     * @return MegaPushNotificationSettings.
     */
    val pushNotificationSetting: MegaPushNotificationSettings
        get() = legacyNotificationRepository.pushNotificationSettings

    /**
     * Method that controls the change in the notifications of a specific chat.
     *
     * @param context Context of Activity.
     * @param option  Muting option selected.
     * @param chatId  Chat ID.
     */
    fun controlMuteNotificationsOfAChat(context: Context?, option: String?, chatId: Long) {
        controlMuteNotifications(context, option, listOf(chatId))
    }

    /**
     * Method that controls the change in general and specific chat notifications.
     *
     * @param context Context of Activity.
     * @param option  Muting option selected
     * @param chatIds   List of Chat ids.
     */
    fun controlMuteNotifications(
        context: Context?,
        option: String?,
        chatIds: List<Long>?
    ) {
        applicationScope.launch {
            when (option) {
                Constants.NOTIFICATIONS_DISABLED -> {
                    chatIds?.forEach { chatId ->
                        notificationsRepository.setChatEnabled(chatId, false)
                    } ?: run {
                        notificationsRepository.setChatsEnabled(false)
                    }
                }

                Constants.NOTIFICATIONS_ENABLED ->
                    chatIds?.forEach { chatId ->
                        notificationsRepository.setChatEnabled(chatId, true)
                    } ?: run {
                        notificationsRepository.setChatsEnabled(true)
                    }

                Constants.NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING, Constants.NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING -> {
                    val timestamp = TimeUtils.getCalendarSpecificTime(option).timeInMillis / 1000
                    chatIds?.forEach { chatId ->
                        notificationsRepository.setChatDoNotDisturb(chatId, timestamp)
                    } ?: run {
                        notificationsRepository.setChatsDoNotDisturb(timestamp)
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

                    chatIds?.forEach { chatId ->
                        notificationsRepository.setChatDoNotDisturb(chatId, time)
                    } ?: run {
                        notificationsRepository.setChatsDoNotDisturb(time)
                    }
                }
            }
            ChatUtil.muteChat(context, option)
        }
    }
}
