package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageRequest
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Use case to update a pending message
 */
class UpdatePendingMessageUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(updatePendingMessageRequest: UpdatePendingMessageRequest) {
        chatMessageRepository.updatePendingMessage(updatePendingMessageRequest)
    }
}