package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for broadcasting when a specific call has ended.
 */
class BroadcastCallEndedUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param callId    ID of the call.
     */
    suspend operator fun invoke(callId: Long) =
        callRepository.broadcastCallEnded(callId)
}