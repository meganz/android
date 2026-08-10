package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.repository.TimeSystemRepository
import java.util.Calendar
import javax.inject.Inject

/**
 * Mute or unmute chat push notifications. `null` or empty [chatIds] targets all chats.
 */
class MuteChatPushNotificationUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
    private val timeSystemRepository: TimeSystemRepository,
) {
    suspend operator fun invoke(
        chatIds: List<Long>?,
        muteOption: ChatPushNotificationMuteOption,
    ) {
        when (muteOption) {
            ChatPushNotificationMuteOption.Mute,
            ChatPushNotificationMuteOption.MuteUntilTurnBackOn,
                -> setEnabled(chatIds, enabled = false)

            ChatPushNotificationMuteOption.Unmute ->
                setEnabled(chatIds, enabled = true)

            ChatPushNotificationMuteOption.Mute30Minutes,
            ChatPushNotificationMuteOption.Mute1Hour,
            ChatPushNotificationMuteOption.Mute6Hours,
            ChatPushNotificationMuteOption.Mute24Hours,
                -> setDoNotDisturb(chatIds, getSpecificPeriodTime(muteOption))

            ChatPushNotificationMuteOption.MuteUntilThisMorning,
            ChatPushNotificationMuteOption.MuteUntilTomorrowMorning,
                -> setDoNotDisturb(chatIds, getMorningTime(muteOption))
        }
    }

    private suspend fun setEnabled(chatIds: List<Long>?, enabled: Boolean) {
        if (chatIds.isNullOrEmpty()) {
            notificationsRepository.setChatsEnabled(enabled)
        } else {
            notificationsRepository.setChatEnabled(chatIds, enabled)
        }
    }

    private suspend fun setDoNotDisturb(chatIds: List<Long>?, timestamp: Long) {
        if (chatIds.isNullOrEmpty()) {
            notificationsRepository.setChatsDoNotDisturb(timestamp)
        } else {
            notificationsRepository.setChatDoNotDisturb(chatIds, timestamp)
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
