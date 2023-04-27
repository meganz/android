package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Creates ChatRoom
 */
class CreateChatRoomUseCase @Inject constructor(private val chatRepository: ChatRepository) {

    /**
     * Invoke.
     *
     * @param isGroup     True if is should create a group chat, false otherwise.
     * @param userHandles List of user handles.
     * @return The chat conversation handle.
     */
    suspend operator fun invoke(isGroup: Boolean, userHandles: List<Long>) =
        chatRepository.createChat(isGroup = isGroup, userHandles = userHandles)
}