package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.management.CallStartedMessage
import javax.inject.Inject

internal class CreateCallStartedMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(message: ChatMessage, isMine: Boolean) = CallStartedMessage(
        message.msgId,
        message.timestamp,
        isMine = isMine
    )
}