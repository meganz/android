package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Get user online status from chat id use case
 */
class GetUserOnlineStatusByChatIdUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val contactsRepository: ContactsRepository,
) {
    /**
     * Invoke
     *
     * @param chatId Chat id is the reference id to the use
     */
    suspend operator fun invoke(chatId: Long): UserStatus {
        val chatListItem = chatRepository.getChatListItem(chatId)
        return when {
            chatListItem == null -> error("ChatListItem does not exist")
            chatListItem.isGroup -> error("ChatListItem is a group")
            else -> contactsRepository.getUserOnlineStatusByHandle(chatListItem.peerHandle)
        }
    }
}
