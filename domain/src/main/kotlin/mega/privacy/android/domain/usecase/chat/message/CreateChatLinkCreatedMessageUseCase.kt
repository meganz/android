package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.management.ChatLinkCreatedMessage
import javax.inject.Inject

internal class CreateChatLinkCreatedMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override fun invoke(message: ChatMessage, isMine: Boolean) = ChatLinkCreatedMessage(
        message.msgId,
        message.timestamp,
        isMine = isMine
    )
}