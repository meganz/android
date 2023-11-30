package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import javax.inject.Inject

internal class CreateNormalChatMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {
    //To be implemented the different type of normal messages. Check [NormalMessage].
    override fun invoke(message: ChatMessage, isMine: Boolean) = TextMessage(
        message.msgId,
        message.timestamp,
        isMine = isMine,
        content = message.content
    )
}