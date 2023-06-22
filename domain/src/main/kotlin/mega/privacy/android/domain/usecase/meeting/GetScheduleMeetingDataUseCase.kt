package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingData
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import javax.inject.Inject

/**
 * Use case to get schedule meeting data
 *
 * @property getScheduledMeetingByChat
 * @property getNextSchedMeetingOccurrence
 * @property getChatRoom
 */
class GetScheduleMeetingDataUseCase @Inject constructor(
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val getNextSchedMeetingOccurrence: GetNextSchedMeetingOccurrenceUseCase,
    private val getChatRoom: GetChatRoom,
) {

    /**
     * Retrieve Schedule Meeting Data given a ChatId
     *
     * @param chatId            Chat id
     * @param meetingTimeMapper Timestamp mapper
     * @return                  [ScheduledMeetingData]
     */
    suspend operator fun invoke(
        chatId: Long,
        meetingTimeMapper: (Long, Long) -> String,
    ): ScheduledMeetingData? =
        getScheduledMeetingByChat(chatId)
            ?.firstOrNull { !it.isCanceled && it.parentSchedId == -1L }
            ?.let { schedMeeting ->
                val chatRoom = getChatRoom(chatId) ?: error("Chat room does not exist")
                val isPending = chatRoom.isActive && schedMeeting.isPending()
                val isRecurringDaily = schedMeeting.rules?.freq == OccurrenceFrequencyType.Daily
                val isRecurringWeekly = schedMeeting.rules?.freq == OccurrenceFrequencyType.Weekly
                val isRecurringMonthly = schedMeeting.rules?.freq == OccurrenceFrequencyType.Monthly
                var startTimestamp = schedMeeting.startDateTime
                var endTimestamp = schedMeeting.endDateTime

                if (isPending && schedMeeting.rules != null) {
                    runCatching { getNextSchedMeetingOccurrence(chatId) }.getOrNull()?.let {
                        startTimestamp = it.startDateTime
                        endTimestamp = it.endDateTime
                    }
                }
                val formattedTimestamp = if (startTimestamp != null && endTimestamp != null) {
                    meetingTimeMapper(startTimestamp!!, endTimestamp!!)
                } else {
                    null
                }

                ScheduledMeetingData(
                    schedId = schedMeeting.schedId,
                    scheduledStartTimestamp = startTimestamp,
                    scheduledEndTimestamp = endTimestamp,
                    scheduledTimestampFormatted = formattedTimestamp,
                    isRecurringDaily = isRecurringDaily,
                    isRecurringWeekly = isRecurringWeekly,
                    isRecurringMonthly = isRecurringMonthly,
                    isPending = isPending,
                )
            }
}
