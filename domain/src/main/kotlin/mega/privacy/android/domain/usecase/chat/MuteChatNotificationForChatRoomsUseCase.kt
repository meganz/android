package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.repository.TimeSystemRepository
import java.util.Calendar
import javax.inject.Inject

/**
 * Use case to mute the chat push notifications for chat rooms
 *
 * @property notificationsRepository
 * @property timeSystemRepository
 */
class MuteChatNotificationForChatRoomsUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
    private val timeSystemRepository: TimeSystemRepository,
) {
    /**
     * Invoke the use case
     *
     * @param chatIdList    Id of the chat room
     * @param muteOption    [ChatPushNotificationMuteOption] to mute the chat
     */
    suspend operator fun invoke(
        chatIdList: List<Long>,
        muteOption: ChatPushNotificationMuteOption,
    ) {
        if (chatIdList.isEmpty()) return

        when (muteOption) {
            ChatPushNotificationMuteOption.Mute, ChatPushNotificationMuteOption.MuteUntilTurnBackOn -> {
                notificationsRepository.setChatEnabled(chatIdList, false)
            }

            ChatPushNotificationMuteOption.Unmute -> {
                notificationsRepository.setChatEnabled(chatIdList, true)
            }

            ChatPushNotificationMuteOption.Mute30Minutes, ChatPushNotificationMuteOption.Mute1Hour, ChatPushNotificationMuteOption.Mute6Hours, ChatPushNotificationMuteOption.Mute24Hours -> {
                notificationsRepository.setChatDoNotDisturb(
                    chatIdList,
                    getSpecificPeriodTime(muteOption)
                )
            }

            ChatPushNotificationMuteOption.MuteUntilThisMorning, ChatPushNotificationMuteOption.MuteUntilTomorrowMorning -> {
                val time = getMorningTime(muteOption)
                notificationsRepository.setChatDoNotDisturb(chatIdList, time)
            }
        }
    }

    private fun getMorningTime(muteOption: ChatPushNotificationMuteOption): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timeSystemRepository.getCurrentTimeInMillis()
            set(Calendar.HOUR_OF_DAY, GetChatMuteOptionListUseCase.MUTE_DIVIDER_HOUR)
            set(Calendar.MINUTE, GetChatMuteOptionListUseCase.MUTE_DIVIDER_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (muteOption) {
            ChatPushNotificationMuteOption.MuteUntilThisMorning -> Unit
            ChatPushNotificationMuteOption.MuteUntilTomorrowMorning ->
                calendar.add(Calendar.DAY_OF_MONTH, 1)

            else -> throw IllegalArgumentException("Invalid mute option")
        }

        return calendar.timeInMillis / 1000
    }

    private fun getSpecificPeriodTime(muteOption: ChatPushNotificationMuteOption): Long =
        Calendar.getInstance().apply {
            timeInMillis = timeSystemRepository.getCurrentTimeInMillis()
            when (muteOption) {
                ChatPushNotificationMuteOption.Mute30Minutes -> add(Calendar.MINUTE, 30)
                ChatPushNotificationMuteOption.Mute1Hour -> add(Calendar.HOUR, 1)
                ChatPushNotificationMuteOption.Mute6Hours -> add(Calendar.HOUR, 6)
                ChatPushNotificationMuteOption.Mute24Hours -> add(Calendar.HOUR, 24)
                else -> throw IllegalArgumentException("Invalid mute option")
            }
        }.timeInMillis / 1000
}
