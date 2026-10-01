package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for monitoring when should leave a chat.
 */
class MonitorLeaveChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke() = chatRepository.monitorLeaveChat()
}