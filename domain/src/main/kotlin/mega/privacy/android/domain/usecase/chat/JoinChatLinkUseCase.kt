package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for joining a chat link as participant.
 */
class JoinChatLinkUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke.
     *
     * @param chatId Chat ID.
     */
    suspend operator fun invoke(chatId: Long) =
        chatRepository.autojoinPublicChat(chatId)
}