package mega.privacy.android.domain.usecase.transfers.chatuploads

import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Monitors the pending messages compression progress
 */
class MonitorPendingMessagesCompressionProgressUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository
) {
    /**
     * Invoke
     * @return a flow with updated map of pending message id to its compression progress
     */
    operator fun invoke() = chatMessageRepository.monitorPendingMessagesCompressionProgress()
}