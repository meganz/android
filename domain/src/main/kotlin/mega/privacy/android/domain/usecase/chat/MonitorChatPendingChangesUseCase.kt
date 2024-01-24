package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Monitor chat pending changes use case
 *
 */
class MonitorChatPendingChangesUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    /**
     * Invoke
     *
     */
    operator fun invoke(chatId: Long) =
        repository.monitorChatPendingChanges(chatId)
}