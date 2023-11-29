package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for broadcasting when the call recording consent has been accepted.
 */
class BroadcastCallRecordingConsentAcceptedUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = callRepository.broadcastCallRecordingConsentAccepted()
}