package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.RevokeNodeAttachmentMessage
import javax.inject.Inject


internal class CreateRevokeNodeAttachmentMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override fun invoke(message: ChatMessage, isMine: Boolean) = RevokeNodeAttachmentMessage(
        message.msgId,
        message.timestamp,
        isMine = isMine
    )
}