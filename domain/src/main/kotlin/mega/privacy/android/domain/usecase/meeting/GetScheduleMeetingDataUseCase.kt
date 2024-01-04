package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingData
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import java.time.Instant
import javax.inject.Inject

/**
 * Use case to get schedule meeting data
 *
 * @property getScheduledMeetingByChatUseCase
 * @property getNextSchedMeetingOccurrenceUseCase
 * @property getChatRoomUseCase
 */
class GetScheduleMeetingDataUseCase @Inject constructor(
    private val getScheduledMeetingByChatUseCase: GetScheduledMeetingByChat,
    private val getNextSchedMeetingOccurrenceUseCase: GetNextSchedMeetingOccurrenceUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
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
    ): ScheduledMeetingData = coroutineScope {
        val schedMeetingJob = async { getScheduledMeeting(chatId) }
        val occurrenceJob = async { getNextSchedMeetingOccurrence(chatId) }
        val isChatRoomActiveJob = async { isChatRoomActive(chatId) }

        val schedMeeting = schedMeetingJob.await()
        val isRecurringDaily = schedMeeting.rules?.freq == OccurrenceFrequencyType.Daily
        val isRecurringWeekly = schedMeeting.rules?.freq == OccurrenceFrequencyType.Weekly
        val isRecurringMonthly = schedMeeting.rules?.freq == OccurrenceFrequencyType.Monthly

        val occurrence = occurrenceJob.await()
        val startTimestamp = occurrence?.startDateTime ?: schedMeeting.startDateTime
        val endTimestamp = occurrence?.endDateTime ?: schedMeeting.endDateTime

        val isChatRoomActive = isChatRoomActiveJob.await()
        val isPending = isMeetingPending(startTimestamp, endTimestamp) && isChatRoomActive
        val formattedTimestamp = if (startTimestamp != null && endTimestamp != null)
            meetingTimeMapper(startTimestamp, endTimestamp)
        else null

        ScheduledMeetingData(
            schedId = schedMeeting.schedId,
            title = schedMeeting.title,
            scheduledStartTimestamp = startTimestamp,
            scheduledEndTimestamp = endTimestamp,
            scheduledTimestampFormatted = formattedTimestamp,
            isRecurringDaily = isRecurringDaily,
            isRecurringWeekly = isRecurringWeekly,
            isRecurringMonthly = isRecurringMonthly,
            isPending = isPending,
        )
    }

    private suspend fun isChatRoomActive(chatId: Long): Boolean =
        getChatRoomUseCase(chatId)?.isActive ?: error("Chat room does not exist")

    private suspend fun getScheduledMeeting(chatId: Long): ChatScheduledMeeting =
        (getScheduledMeetingByChatUseCase(chatId) ?: error("Scheduled Meeting does not exist"))
            .firstOrNull { !it.isCanceled && it.parentSchedId == -1L } ?: error("Invalid Meeting")

    private suspend fun getNextSchedMeetingOccurrence(chatId: Long): ChatScheduledMeetingOccurr? =
        runCatching { getNextSchedMeetingOccurrenceUseCase(chatId) }.getOrNull()

    private fun isMeetingPending(
        startTimestamp: Long?,
        endTimestamp: Long?,
    ): Boolean {
        val now = Instant.now()
        return startTimestamp?.toInstant()?.isAfter(now) == true
                || endTimestamp?.toInstant()?.isAfter(now) == true
    }

    private fun Long.toInstant(): Instant =
        Instant.ofEpochSecond(this)
}
