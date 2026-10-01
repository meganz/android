package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for broadcasting when should leave a chat.
 */
class BroadcastLeaveChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke
     *
     * @param chatId [Long] ID of the chat to leave.
     */
    suspend operator fun invoke(chatId: Long) = chatRepository.broadcastLeaveChat(chatId)
}