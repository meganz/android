package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.meeting.ResultOccurrenceUpdate

/**
 * Use case for monitoring updates on scheduled meetings occurrences
 */
fun interface MonitorScheduledMeetingOccurrencesUpdates {

    /**
     * Invoke.
     *
     * @return          Flow of [ResultOccurrenceUpdate].
     */
    suspend operator fun invoke(): Flow<ResultOccurrenceUpdate>
}