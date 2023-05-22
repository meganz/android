package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for monitoring when a chat is archived.
 */
class MonitorChatArchivedUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /**
     * Invoke
     *
     * @return Flow [String]
     */
    operator fun invoke() = chatRepository.monitorChatArchived()
}