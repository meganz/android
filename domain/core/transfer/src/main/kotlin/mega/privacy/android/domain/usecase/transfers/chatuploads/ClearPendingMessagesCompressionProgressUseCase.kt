package mega.privacy.android.domain.usecase.transfers.chatuploads

import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Clears the progress for all the pending messages
 */
class ClearPendingMessagesCompressionProgressUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke() = chatMessageRepository.clearPendingMessagesCompressionProgress()
}