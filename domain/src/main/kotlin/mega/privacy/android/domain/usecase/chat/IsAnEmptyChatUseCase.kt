package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case to check if the chat is empty
 */
class IsAnEmptyChatUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    /**
     * Invoke.
     *
     * @return True, if the chat is empty. False, if not.
     */
    suspend operator fun invoke(chatId: Long): Boolean {
        val messageType =
            chatRepository.getChatListItem(chatId)?.lastMessageType ?: ChatRoomLastMessage.Invalid

        return messageType == ChatRoomLastMessage.Invalid || messageType == ChatRoomLastMessage.Unknown
    }
}