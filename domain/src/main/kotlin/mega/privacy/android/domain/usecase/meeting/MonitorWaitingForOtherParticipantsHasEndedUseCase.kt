package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for monitoring when waiting for others participants has ended
 */
class MonitorWaitingForOtherParticipantsHasEndedUseCase @Inject constructor(
    private val callRepository: CallRepository
) {
    /**
     * Invoke
     */
    operator fun invoke() = callRepository.monitorWaitingForOtherParticipantsHasEnded()
}