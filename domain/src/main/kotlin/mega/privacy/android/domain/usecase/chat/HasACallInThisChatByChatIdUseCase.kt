package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for checking if there is a call in a concrete chat.
 *
 * @property chatRepository [ChatRepository].
 */
class HasACallInThisChatByChatIdUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke
     *
     * @param chatId Chat identifier for checking.
     */
    suspend operator fun invoke(chatId: Long) = chatRepository.hasCallInChatRoom(chatId)
}