package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Ring a participant in chatroom with an ongoing call that they didn't pick up
 */
class RingIndividualInACallUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Ring a participant in chatroom with an ongoing call that they didn't pick up
     *
     * @param chatId        The chat ID
     * @param userId        The participant user ID
     * @return              [ChatRequest]
     */
    suspend operator fun invoke(
        chatId: Long,
        userId: Long,
    ): ChatRequest = callRepository.ringIndividualInACall(
        chatId = chatId, userId = userId,
    )
}