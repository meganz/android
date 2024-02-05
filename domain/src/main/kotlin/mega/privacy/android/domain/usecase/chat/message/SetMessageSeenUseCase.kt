package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Set Message Seen Use Case
 *
 */
class SetMessageSeenUseCase @Inject constructor(
    private val repository: ChatMessageRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(
        chatId: Long,
        messageId: Long,
    ) = repository.setMessageSeen(chatId, messageId)
}