package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.management.CallEndedMessage
import javax.inject.Inject

internal class CreateCallEndedMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(message: ChatMessage, isMine: Boolean) = CallEndedMessage(
        message.msgId,
        message.timestamp,
        isMine = isMine,
        message.termCode,
        message.duration.toLong(),
    )
}