package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.management.TitleChangeMessage
import javax.inject.Inject


internal class CreateTitleChangeMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(message: ChatMessage, isMine: Boolean) = TitleChangeMessage(
        message.msgId,
        message.timestamp,
        isMine = isMine
    )
}