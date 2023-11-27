package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.repository.TimeSystemRepository
import javax.inject.Inject

/**
 * Use case to get the option list of  when to restore muted chat notifications.
 */
class GetChatMuteOptionListUseCase @Inject constructor(
    private val timeSystemRepository: TimeSystemRepository,
) {
    /**
     *
     * @param chatList List of chat ids to get the conditional option.
     * @return List of [ChatPushNotificationMuteOption]
     */
    operator fun invoke(chatList: List<Long>): List<ChatPushNotificationMuteOption> {
        val conditionalOption = if (chatList.isNotEmpty()) {
            ChatPushNotificationMuteOption.MuteUntilTurnBackOn
        } else {
            getUntilMorningConditionalOption()
        }
        return listOf(
            ChatPushNotificationMuteOption.Mute30Minutes,
            ChatPushNotificationMuteOption.Mute1Hour,
            ChatPushNotificationMuteOption.Mute6Hours,
            ChatPushNotificationMuteOption.Mute24Hours,
            conditionalOption,
        )
    }

    private fun getUntilMorningConditionalOption(): ChatPushNotificationMuteOption {
        val hour = timeSystemRepository.getCurrentHourOfDay()
        val minute = timeSystemRepository.getCurrentMinute()
        return if (hour < MUTE_DIVIDER_HOUR || (hour == MUTE_DIVIDER_HOUR && minute == MUTE_DIVIDER_MINUTE))
            ChatPushNotificationMuteOption.MuteUntilThisMorning
        else
            ChatPushNotificationMuteOption.MuteUntilTomorrowMorning
    }

    companion object {
        /**
         * Hour of the day to decide whether it is morning or not.
         */
        const val MUTE_DIVIDER_HOUR = 8

        /**
         * Minute of the hour to decide whether it is morning or not.
         * This is used together with [MUTE_DIVIDER_HOUR].
         */
        const val MUTE_DIVIDER_MINUTE = 0
    }
}