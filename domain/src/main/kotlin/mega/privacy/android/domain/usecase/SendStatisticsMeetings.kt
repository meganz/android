package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.statistics.MeetingsStatisticsEvents

/**
 * Send Statistics event for Meetings
 */
fun interface SendStatisticsMeetings {
    /**
     * Invoke the use case
     *
     **/
    suspend operator fun invoke(event: MeetingsStatisticsEvents)
}