package mega.privacy.android.domain.usecase.chat

import javax.inject.Inject

/**
 * Use Case to check if user has archived Chat Rooms
 *
 * @property getArchivedChatRoomsUseCase
 */
class HasArchivedChatsUseCase @Inject constructor(
    private val getArchivedChatRoomsUseCase: GetArchivedChatRoomsUseCase,
) {

    /**
     * Check for existing archived chats
     *
     * @return  True if there are archived chat rooms, false otherwise.
     */
    suspend operator fun invoke(): Boolean = getArchivedChatRoomsUseCase().isNotEmpty()
}
