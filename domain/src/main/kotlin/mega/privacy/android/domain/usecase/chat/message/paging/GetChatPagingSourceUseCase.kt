package mega.privacy.android.domain.usecase.chat.message.paging

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Get chat paging source use case
 *
 * @property chatRepository
 */
class GetChatPagingSourceUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    /**
     * Invoke
     *
     * @param chatId
     */
    operator fun invoke(chatId: Long) = chatRepository.getPagedMessages(chatId)
}