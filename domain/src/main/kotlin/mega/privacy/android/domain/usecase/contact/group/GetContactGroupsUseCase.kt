package mega.privacy.android.domain.usecase.contact.group

import mega.privacy.android.domain.entity.contacts.group.ContactGroup
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Get contact groups use case
 *
 * @property chatRepository
 * @property getContactGroupAvatarsUseCase
 */
class GetContactGroupsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val getContactGroupAvatarsUseCase: GetContactGroupAvatarsUseCase,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(): List<ContactGroup> {
        val groupRooms = chatRepository.getChatRooms()
            .filter { it.isGroup && it.peerCount > 0 }
        val avatarsByChat = getContactGroupAvatarsUseCase(groupRooms)
        return groupRooms
            .map { room ->
                ContactGroup(
                    chatId = room.chatId,
                    title = room.title,
                    avatar = avatarsByChat[room.chatId].orEmpty(),
                    isPublic = room.isPublic,
                )
            }
            .sortedWith(
                compareBy(
                    String.CASE_INSENSITIVE_ORDER,
                    ContactGroup::title
                )
            )
    }
}
