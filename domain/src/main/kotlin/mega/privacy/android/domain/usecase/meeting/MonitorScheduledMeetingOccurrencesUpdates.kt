package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.flow.Flow

/**
 * Use case for monitoring updates on scheduled meetings occurrences
 */
fun interface MonitorScheduledMeetingOccurrencesUpdates {

    /**
     * Invoke.
     *
     * @return          Flow of [Long].
     */
    suspend operator fun invoke(): Flow<Long>
}