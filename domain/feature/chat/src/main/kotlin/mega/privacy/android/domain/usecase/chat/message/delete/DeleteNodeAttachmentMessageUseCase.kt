package mega.privacy.android.domain.usecase.chat.message.delete

import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import javax.inject.Inject

/**
 * Use case to delete a node attachment message.
 */
class DeleteNodeAttachmentMessageUseCase @Inject constructor(
    private val revokeAttachmentMessageUseCase: RevokeAttachmentMessageUseCase,
) : DeleteMessageUseCase() {

    override suspend fun deleteMessage(message: TypedMessage) {
        revokeAttachmentMessageUseCase(message.chatId, message.msgId)
    }

    override suspend fun canDelete(message: TypedMessage) =
        message.isDeletable && message is NodeAttachmentMessage
}