package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for monitoring if a chat is in leaving state.
 */
class MonitorLeavingChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke.
     *
     * @param chatId Chat id.
     */
    operator fun invoke(chatId: Long) = chatRepository.monitorLeavingChat(chatId)
}