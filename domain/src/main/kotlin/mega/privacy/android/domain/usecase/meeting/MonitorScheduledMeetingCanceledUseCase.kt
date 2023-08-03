package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for monitoring when a recurring scheduled meeting with a single occurrence is canceled.
 */
class MonitorSingleOccurrenceScheduledMeetingCancelledUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @return Flow [Int]
     */
    operator fun invoke() = callRepository.monitorScheduledMeetingCanceled()
}