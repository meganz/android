package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import javax.inject.Inject

/**
 * Create normal chat message use case.
 */
class CreateNormalChatMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {
    //To be implemented the different type of normal messages. Check [NormalMessage].
    override fun invoke(message: ChatMessage, isMine: Boolean) = TextMessage(
        msgId = message.msgId,
        time = message.timestamp,
        isMine = isMine,
        userHandle = message.userHandle,
        tempId = message.tempId,
        content = message.content
    )
}