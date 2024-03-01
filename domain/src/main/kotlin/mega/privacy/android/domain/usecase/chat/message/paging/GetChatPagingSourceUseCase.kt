package mega.privacy.android.domain.usecase.chat.message.paging

import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Get chat paging source use case
 *
 * @property chatMessageRepository
 */
class GetChatPagingSourceUseCase @Inject constructor(private val chatMessageRepository: ChatMessageRepository) {
    /**
     * Invoke
     *
     * @param chatId
     */
    operator fun invoke(chatId: Long) = chatMessageRepository.getPagedMessages(chatId)
}