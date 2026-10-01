package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for broadcasting when scheduled meeting is canceled.
 */
class BroadcastScheduledMeetingCanceledUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param messageResId [Int]
     */
    suspend operator fun invoke(messageResId: Int) =
        callRepository.broadcastScheduledMeetingCanceled(messageResId)
}