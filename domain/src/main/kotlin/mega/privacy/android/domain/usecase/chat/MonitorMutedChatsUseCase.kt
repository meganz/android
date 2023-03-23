package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for monitoring muted chats.
 *
 */
class MonitorMutedChatsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke.
     *
     * @return Flow of Boolean.
     */
    operator fun invoke() = chatRepository.monitorMutedChats()
}