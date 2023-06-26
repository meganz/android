package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use Case to check if user has archived Chat Rooms
 *
 * @property chatRepository
 */
class HasArchivedChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Check for existing archived chats
     *
     * @return  True if there are archived chat rooms, false otherwise.
     */
    suspend operator fun invoke(): Boolean =
        chatRepository.getArchivedChatRooms().isNotEmpty()
}
