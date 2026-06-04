package mega.privacy.android.domain.usecase.contact.group

import mega.privacy.android.domain.entity.contacts.group.ContactGroup
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.chat.GetChatGroupAvatarUseCase
import javax.inject.Inject

/**
 * Get contact groups use case
 *
 * @property chatRepository
 * @property getChatGroupAvatarUseCase
 */
class GetContactGroupsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val getChatGroupAvatarUseCase: GetChatGroupAvatarUseCase,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(): List<ContactGroup> =
        chatRepository.getChatRooms()
            .filter { it.isGroup && it.peerCount > 0 }
            .map { chatRoom ->
                ContactGroup(
                    chatId = chatRoom.chatId,
                    title = chatRoom.title,
                    avatar = getChatGroupAvatarUseCase(chatRoom.chatId),
                    isPublic = chatRoom.isPublic
                )
            }
            .sortedWith(
                compareBy(
                    String.CASE_INSENSITIVE_ORDER,
                    ContactGroup::title
                )
            )

}
