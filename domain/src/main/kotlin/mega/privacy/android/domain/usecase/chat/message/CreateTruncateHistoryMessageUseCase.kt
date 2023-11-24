package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.management.TruncateHistoryMessage
import javax.inject.Inject

internal class CreateTruncateHistoryMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override fun invoke(message: ChatMessage, isMine: Boolean) = TruncateHistoryMessage(
        message.msgId,
        message.timestamp,
        isMine = isMine
    )
}