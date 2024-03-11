package mega.privacy.android.domain.usecase.chat.message.delete

import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.InvalidMessage
import mega.privacy.android.domain.entity.chat.messages.meta.MetaMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Use case to delete a general message.
 */
class DeleteGeneralMessageUseCase @Inject constructor(
    private val chatMessageRepository: ChatMessageRepository,
) : DeleteMessageUseCase() {

    public override suspend fun deleteMessage(message: TypedMessage) {
        chatMessageRepository.deleteMessage(message.chatId, message.msgId)
    }

    override suspend fun canDelete(message: TypedMessage) =
        message.isDeletable &&
                (message is InvalidMessage
                        || message is MetaMessage
                        || message is NormalMessage
                        || message is ContactAttachmentMessage)
}