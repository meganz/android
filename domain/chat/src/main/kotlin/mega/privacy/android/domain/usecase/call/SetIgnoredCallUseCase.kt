package mega.privacy.android.domain.usecase.call

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Mark as ignored the call associated with a chatroom
 */
class SetIgnoredCallUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param chatId Chat id
     * @return                  [ChatRequest]
     */
    suspend operator fun invoke(
        chatId: Long,
    ): Boolean = callRepository.setIgnoredCall(
        chatId,
    )
}