package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for broadcasting when a chat is archived.
 */
class BroadcastChatArchivedUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Invoke
     *
     * @param chatTitle [String]
     */
    suspend operator fun invoke(chatTitle: String) = chatRepository.broadcastChatArchived(chatTitle)
}