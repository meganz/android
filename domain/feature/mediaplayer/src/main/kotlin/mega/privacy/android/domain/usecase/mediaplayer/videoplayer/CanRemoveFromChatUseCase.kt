package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.chat.GetChatMessageUseCase
import javax.inject.Inject

/**
 * Use case for checking if the message can be removed from the chat
 */
class CanRemoveFromChatUseCase @Inject constructor(
    private val getChatMessageUseCase: GetChatMessageUseCase,
    private val chatRepository: ChatRepository,
) {

    /**
     * Check if the message can be removed from the chat
     *
     * @param chatId The chat ID
     * @param messageId The message ID
     * @return True if it can be removed, false otherwise
     */
    suspend operator fun invoke(chatId: Long, messageId: Long): Boolean {
        val message = getChatMessageUseCase(chatId, messageId)
        val myUserHandle = chatRepository.getMyUserHandle()
        return message != null
                && message.userHandle == myUserHandle
                && message.isDeletable
    }
}