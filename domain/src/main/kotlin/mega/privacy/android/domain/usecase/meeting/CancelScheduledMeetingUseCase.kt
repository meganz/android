package mega.privacy.android.domain.usecase.meeting

import javax.inject.Inject

/**
 * Cancel a scheduled meeting
 */
class CancelScheduledMeetingUseCase @Inject constructor(
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val updateScheduledMeetingUseCase: UpdateScheduledMeetingUseCase,
) {
    /**
     * Invoke
     *
     * @param chatId MegaChatHandle that identifies a chat room
     * */
    suspend operator fun invoke(chatId: Long) {
        getScheduledMeetingByChat(chatId)
            ?.firstOrNull { !it.isCanceled && it.parentSchedId == -1L }
            ?.let { schedMeeting ->
                updateScheduledMeetingUseCase(
                    chatId = schedMeeting.chatId,
                    schedId = schedMeeting.schedId,
                    timezone = schedMeeting.timezone.orEmpty(),
                    startDate = schedMeeting.startDateTime!!,
                    endDate = schedMeeting.endDateTime!!,
                    title = schedMeeting.title.orEmpty(),
                    description = schedMeeting.description.orEmpty(),
                    cancelled = true,
                    flags = schedMeeting.flags,
                    rules = schedMeeting.rules
                )
            }
    }
}