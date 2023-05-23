package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for monitoring when successfully joined to a chat.
 */
class MonitorJoinedSuccessfullyUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke() = chatRepository.monitorJoinedSuccessfully()
}