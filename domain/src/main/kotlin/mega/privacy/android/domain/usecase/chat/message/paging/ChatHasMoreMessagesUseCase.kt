package mega.privacy.android.domain.usecase.chat.message.paging

import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Chat has more messages use case
 *
 * @property chatRepository
 */
class ChatHasMoreMessagesUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    /**
     * Invoke
     *
     * @param chatId
     */
    suspend operator fun invoke(chatId: Long) =
        chatRepository.getLastLoadResponse(chatId) != ChatHistoryLoadStatus.NONE
}