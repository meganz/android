package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for create chat link
 */
class CreateChatLinkUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invocation method.
     *
     * @param chatId    The chat id.
     * @return          ChatRequest
     */
    suspend operator fun invoke(chatId: Long): ChatRequest =
        chatRepository.createChatLink(chatId = chatId)
}
