package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * MonitorScheduledMeetingOccurrencesUpdatesUseCase
 *
 * Gets scheduled meeting occurrences updates which was part of MegaRequestListener
 */
class MonitorScheduledMeetingOccurrencesUpdatesUseCase @Inject constructor(private val callRepository: CallRepository) {

    /**
     * Invoke
     *
     * Calls monitor scheduled meeting occurrences flow from [CallRepository]
     * @return [Flow<ResultOccurrenceUpdate>]
     */
    operator fun invoke() = callRepository.monitorScheduledMeetingOccurrencesUpdates()
}