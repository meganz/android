package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import javax.inject.Inject


internal class CreateNodeAttachmentMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override fun invoke(message: ChatMessage, isMine: Boolean) = NodeAttachmentMessage(
        message.msgId,
        message.timestamp,
        isMine = isMine
    )
}