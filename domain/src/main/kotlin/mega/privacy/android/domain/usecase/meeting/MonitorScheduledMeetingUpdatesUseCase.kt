package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * MonitorScheduledMeetingUpdatesUseCase
 *
 * Gets scheduled meeting updates which was part of MegaRequestListener
 */
class MonitorScheduledMeetingUpdatesUseCase @Inject constructor(private val callRepository: CallRepository) {

    /**
     * Invoke
     *
     * Calls monitor scheduled meeting flow from [CallRepository]
     * @return [Flow<ChatScheduledMeeting>]
     */
    operator fun invoke() = callRepository.monitorScheduledMeetingUpdates()
}