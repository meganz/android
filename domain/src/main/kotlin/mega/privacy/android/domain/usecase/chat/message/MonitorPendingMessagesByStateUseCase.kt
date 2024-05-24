package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Monitor pending messages by state
 */
class MonitorPendingMessagesByStateUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke(vararg states: PendingMessageState) =
        chatMessageRepository.monitorPendingMessagesByState(states = states)
}