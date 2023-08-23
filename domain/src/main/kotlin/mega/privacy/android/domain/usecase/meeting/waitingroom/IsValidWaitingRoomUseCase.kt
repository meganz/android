package mega.privacy.android.domain.usecase.meeting.waitingroom

import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case to check if a Chat Room is a valid Waiting Room and I'm not a Moderator.
 *
 * @property chatRepository     [ChatRepository]
 */
class IsValidWaitingRoomUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke
     *
     * @param chatId    Chat Room Id to check.
     * @return          True if it's a valid waiting room without privileges, false otherwise.
     */
    suspend operator fun invoke(chatId: Long): Boolean {
        val chatRoom = chatRepository.getChatRoom(chatId) ?: throw ChatRoomDoesNotExistException()
        return chatRoom.isWaitingRoom && chatRoom.ownPrivilege != ChatRoomPermission.Moderator
    }
}
