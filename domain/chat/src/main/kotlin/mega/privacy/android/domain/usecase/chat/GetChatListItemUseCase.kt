package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case to get the [ChatListItem] of a chat room.
 *
 * @property chatRepository [ChatRepository]
 */
class GetChatListItemUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Get the [ChatListItem] of a chat room.
     *
     * @param chatId Chat id of the chat room.
     * @return [ChatListItem] or null if the chat room does not exist.
     */
    suspend operator fun invoke(chatId: Long): ChatListItem? =
        chatRepository.getChatListItem(chatId)
}
