package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.management.PermissionChangeMessage
import javax.inject.Inject


internal class CreatePermissionChangeMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override fun invoke(message: ChatMessage, isMine: Boolean) = PermissionChangeMessage(
        message.msgId,
        message.timestamp,
        isMine = isMine
    )
}