package mega.privacy.android.app.contacts.usecase

import androidx.core.net.toUri
import mega.privacy.android.app.contacts.group.data.ContactGroupItem
import mega.privacy.android.app.contacts.group.data.ContactGroupUser
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Use Case to retrieve contact groups for current user.
 */
class GetContactGroupsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val avatarRepository: AvatarRepository,
    private val contactsRepository: ContactsRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(): List<ContactGroupItem> {
        val groups = mutableListOf<ContactGroupItem>()

        chatRepository.getChatRooms().forEach { chatRoom ->
            if (chatRoom.isGroup && chatRoom.peerCount > 0) {
                val firstUserHandle = chatRoom.peerHandlesList.firstOrNull()
                val lastUserHandle = chatRoom.peerHandlesList.lastOrNull()

                if (firstUserHandle != null && lastUserHandle != null) {
                    groups.add(
                        ContactGroupItem(
                            chatId = chatRoom.chatId,
                            title = chatRoom.title,
                            firstUser = getGroupUserFromHandle(
                                firstUserHandle,
                            ),
                            lastUser = getGroupUserFromHandle(
                                lastUserHandle,
                            ),
                            isPublic = chatRoom.isPublic
                        )
                    )
                }
            }
        }

        return groups.sortedWith(
            compareBy(
                String.CASE_INSENSITIVE_ORDER,
                ContactGroupItem::title
            )
        )

    }

    /**
     * Build ContactGroupUser given an User handle
     *
     * @param userHandle    User handle to obtain group
     * @return              ContactGroupUser
     */
    private suspend fun getGroupUserFromHandle(userHandle: Long) =
        ContactGroupUser(
            handle = userHandle,
            email = contactsRepository.getUserEmail(userHandle),
            firstName = contactsRepository.getUserFirstName(userHandle),
            avatar = avatarRepository.getAvatarFile(userHandle).toUri(),
            avatarColor = avatarRepository.getAvatarColor(userHandle),
        )
}
