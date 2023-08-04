package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for monitoring when a recurring scheduled meeting is canceled.
 */
class MonitorScheduledMeetingCanceledUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @return Flow [Int]
     */
    operator fun invoke() = callRepository.monitorScheduledMeetingCanceled()
}