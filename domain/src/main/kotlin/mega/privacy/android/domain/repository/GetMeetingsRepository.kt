package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.meeting.MeetingParticipantsResult
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingResult
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingStatus

/**
 * Get Meetings repository
 */
interface GetMeetingsRepository {

    /**
     * Get meeting participants
     *
     * @param chatId    Meeting to retrieve data from
     * @return          [MeetingParticipantsResult]
     */
    suspend fun getMeetingParticipants(chatId: Long): MeetingParticipantsResult

    /**
     * Get meeting scheduled data
     *
     * @param chatId    Meeting to retrieve data from
     * @return          [ScheduledMeetingResult]
     */
    suspend fun getMeetingScheduleData(chatId: Long): ScheduledMeetingResult?

    /**
     * Get scheduled meeting status
     *
     * @param chatId    Meeting to retrieve data from
     * @return          [ScheduledMeetingStatus]
     */
    suspend fun getScheduledMeetingStatus(chatId: Long): ScheduledMeetingStatus
}
