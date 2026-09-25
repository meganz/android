package mega.privacy.android.domain.usecase.chat.message.paging

import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Clear chat messages use case
 *
 * @property chatRepository
 */
class ClearChatMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatMessageRepository: ChatMessageRepository,
) {
    /**
     * Invoke
     *
     * @param chatId
     */
    suspend operator fun invoke(chatId: Long, clearPendingMessages: Boolean) {
        chatRepository.clearChatMessages(chatId)
        if (clearPendingMessages) chatMessageRepository.clearChatPendingMessages(chatId)
    }
}