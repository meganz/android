package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.management.ChatLinkRemovedMessage
import javax.inject.Inject

internal class CreateChatLinkRemovedMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override fun invoke(message: ChatMessage, isMine: Boolean) = ChatLinkRemovedMessage(
        message.msgId,
        message.timestamp,
        isMine = isMine
    )
}