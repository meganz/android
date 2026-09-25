package mega.privacy.android.domain.usecase.chat.link

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for remove chat link
 */
class RemoveChatLinkUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invocation method.
     *
     * @param chatId    The chat id.
     * @return          ChatRequest
     */
    suspend operator fun invoke(chatId: Long): ChatRequest =
        chatRepository.removeChatLink(chatId = chatId)
}
