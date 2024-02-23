package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Monitor pending messages
 */
class MonitorPendingMessagesUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
    private val createPendingAttachmentMessageUseCase: CreatePendingAttachmentMessageUseCase,
) {

    /**
     * Invoke
     *
     * @param chatId
     *
     * @return a flow with the list of the pending messages of the chat with [chatId]
     */
    operator fun invoke(chatId: Long) = chatMessageRepository.monitorPendingMessages(chatId).map {
        it.map { createPendingAttachmentMessageUseCase(it) }
    }
}