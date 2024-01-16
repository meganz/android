package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case to leave a chat
 *
 * @property chatRepository     [ChatRepository]
 */
class LeaveChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke
     *
     * @param chatId    Chat id
     */
    suspend operator fun invoke(chatId: Long) = chatRepository.leaveChat(chatId)
}