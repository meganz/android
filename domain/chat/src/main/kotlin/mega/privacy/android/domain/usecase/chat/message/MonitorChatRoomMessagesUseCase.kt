package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Monitor chat room messages use case
 *
 * @property chatRepository
 */
class MonitorChatRoomMessagesUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    /**
     * Invoke
     *
     * @param chatId
     */
    operator fun invoke(chatId: Long) = chatRepository.monitorOnMessageLoaded(chatId)
}