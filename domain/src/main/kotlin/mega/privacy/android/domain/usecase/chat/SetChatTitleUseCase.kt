package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Set chat title
 */
class SetChatTitleUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke
     *
     * @param chatId MegaChatHandle that identifies a chat room
     * @param title  Chat room title
     */
    suspend operator fun invoke(
        chatId: Long,
        title: String,
    ): ChatRequest =
        chatRepository.setChatTitle(
            chatId,
            title,
        )
}