package mega.privacy.android.domain.usecase.chat.message.delete

import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Use case to revoke an attachment message.
 */
class RevokeAttachmentMessageUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
) {

    /**
     * Revokes an attachment message.
     *
     * @param chatId    Chat ID.
     * @param msgId     Message ID.
     * @return Chat message to remove in case of success. Null otherwise.
     */
    suspend operator fun invoke(chatId: Long, msgId: Long) =
        chatMessageRepository.revokeAttachmentMessage(chatId, msgId)
}