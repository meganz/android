package mega.privacy.android.app.contacts.usecase

import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.app.contacts.group.data.ContactGroupItem
import mega.privacy.android.app.contacts.group.data.ContactGroupUser
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ChatParticipantsRepository
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
    private val chatParticipantsRepository: ChatParticipantsRepository,
    @IoDispatcher private val coroutineDispatcher: CoroutineDispatcher,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(): List<ContactGroupItem> = coroutineScope {
        chatRepository.getChatRooms()
            .filter { it.isGroup && it.peerCount > 0 }
            .mapNotNull { chatRoom ->
                // align logic with GetChatGroupAvatarUseCase
                val participants =
                    chatParticipantsRepository.getChatParticipantsHandles(chatRoom.chatId, 2)
                        .toMutableList()
                val myUserHandle = chatRepository.getMyUserHandle()
                if (participants.size == 1) {
                    participants.add(0, myUserHandle)
                }
                participants.firstOrNull()?.let { firstHandle ->
                    participants.lastOrNull()?.let { lastHandle ->
                        async(coroutineDispatcher) {
                            val firstUserDeferred = async { getGroupUserFromHandle(firstHandle) }
                            val lastUserDeferred = async { getGroupUserFromHandle(lastHandle) }
                            ContactGroupItem(
                                chatId = chatRoom.chatId,
                                title = chatRoom.title,
                                firstUser = firstUserDeferred.await(),
                                lastUser = lastUserDeferred.await(),
                                isPublic = chatRoom.isPublic
                            )
                        }
                    }
                }
            }
            .awaitAll()
            .sortedWith(
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
    private suspend fun getGroupUserFromHandle(userHandle: Long): ContactGroupUser =
        ContactGroupUser(
            handle = userHandle,
            email = runCatching { contactsRepository.getUserEmail(userHandle) }.getOrNull(),
            firstName = runCatching { contactsRepository.getUserFirstName(userHandle) }.getOrNull(),
            avatar = runCatching { avatarRepository.getAvatarFile(userHandle) }.getOrNull()
                ?.toUri(),
            avatarColor = avatarRepository.getAvatarColor(userHandle),
        )
}
