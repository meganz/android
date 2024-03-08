package mega.privacy.android.domain.usecase.chat.message.delete

import mega.privacy.android.domain.entity.chat.messages.PendingAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Use case to delete a node attachment message.
 */
class DeletePendingMessageUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
) : DeleteMessageUseCase() {

    override suspend fun deleteMessage(message: TypedMessage) {
        (message as? PendingAttachmentMessage)?.let {
            chatMessageRepository.deletePendingMessageById(it.msgId)
            // AND-18365: cancel upload if it's uploading
        }
    }

    override suspend fun canDelete(message: TypedMessage) =
        message is PendingAttachmentMessage
}