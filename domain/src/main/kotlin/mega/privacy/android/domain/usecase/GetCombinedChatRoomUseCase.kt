package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for getting the updated main data of a Combined chat room.
 */
class GetCombinedChatRoomUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke.
     *
     * @param chatId                Chat id.
     * @return [CombinedChatRoom]   containing the updated data.
     */
    suspend operator fun invoke(chatId: Long): CombinedChatRoom? =
        chatRepository.getCombinedChatRoom(chatId)
}
