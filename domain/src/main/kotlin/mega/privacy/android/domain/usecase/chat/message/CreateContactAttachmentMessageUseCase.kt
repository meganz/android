package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import javax.inject.Inject

internal class CreateContactAttachmentMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override fun invoke(message: ChatMessage, isMine: Boolean) = ContactAttachmentMessage(
        message.msgId,
        message.timestamp,
        isMine = isMine
    )
}