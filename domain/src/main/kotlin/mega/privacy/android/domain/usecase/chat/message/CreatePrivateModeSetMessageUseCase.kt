package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.management.PrivateModeSetMessage
import javax.inject.Inject


internal class CreatePrivateModeSetMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override fun invoke(message: ChatMessage, isMine: Boolean) = PrivateModeSetMessage(
        message.msgId,
        message.timestamp,
        isMine = isMine
    )
}