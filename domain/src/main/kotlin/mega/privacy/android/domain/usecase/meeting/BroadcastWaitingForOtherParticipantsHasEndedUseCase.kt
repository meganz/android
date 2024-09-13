package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case for broadcasting when waiting for other participants has ended
 */
class BroadcastWaitingForOtherParticipantsHasEndedUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(chatId:Long, isEnded: Boolean) =
        callRepository.broadcastWaitingForOtherParticipantsHasEnded(chatId, isEnded)
}