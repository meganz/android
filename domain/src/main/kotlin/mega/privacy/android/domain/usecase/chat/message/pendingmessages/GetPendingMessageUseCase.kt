package mega.privacy.android.domain.usecase.chat.message.pendingmessages

import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Use case to get a pending message by its ID.
 */
class GetPendingMessageUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke(pendingMessageId: Long) =
        chatMessageRepository.getPendingMessage(pendingMessageId)
}