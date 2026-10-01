package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for getting the updated main data of a chat room.
 */
class GetChatRoomUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke.
     *
     * @param chatId        Chat id.
     * @return [ChatRoom]   containing the updated data.
     */
    suspend operator fun invoke(chatId: Long) = chatRepository.getChatRoom(chatId)
}