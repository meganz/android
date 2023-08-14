package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Create group ChatRoom Use Case
 */
class CreateGroupChatRoomUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Create Group Chat Room
     *
     * @param emails            Contacts email
     * @param title             Room title
     * @param isEkr             Flag to check if it's a private chat room
     * @param addParticipants   Flag to check if other users can be invited into the chat
     * @param chatLink          Flag to create a chat link
     * @return                  Chat conversation Handle
     */
    suspend operator fun invoke(
        emails: List<String>,
        title: String?,
        isEkr: Boolean,
        addParticipants: Boolean,
        chatLink: Boolean,
    ): Long = if (isEkr) {
        chatRepository.createGroupChat(
            userHandles = emails.mapNotNull { chatRepository.getContactHandle(it) },
            title = title,
            speakRequest = false,
            waitingRoom = false,
            openInvite = addParticipants,
        )
    } else {
        chatRepository.createPublicChat(
            userHandles = emails.mapNotNull { chatRepository.getContactHandle(it) },
            title = title,
            speakRequest = false,
            waitingRoom = false,
            openInvite = addParticipants,
        ).also { if (chatLink) chatRepository.createChatLink(it) }
    }
}
