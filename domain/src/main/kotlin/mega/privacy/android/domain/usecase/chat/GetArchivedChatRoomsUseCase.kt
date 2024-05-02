package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case to get the archived chat rooms
 *
 * @property chatRepository
 */
class GetArchivedChatRoomsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invocation method.
     *
     * @return List of combined chat rooms
     */
    suspend operator fun invoke() = chatRepository.getArchivedChatRooms()
}
