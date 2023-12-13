package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageCode
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.messages.InvalidMessage
import mega.privacy.android.domain.entity.chat.messages.InvalidMessageType
import javax.inject.Inject

/**
 * Create invalid message use case.
 */
class CreateInvalidMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(message: ChatMessage, isMine: Boolean) = InvalidMessage(
        msgId = message.msgId,
        time = message.timestamp,
        isMine = isMine,
        userHandle = message.userHandle,
        type = if (message.type == ChatMessageType.INVALID) {
            InvalidMessageType.Unrecognizable
        } else {
            when (message.code) {
                ChatMessageCode.INVALID_FORMAT -> InvalidMessageType.Format
                ChatMessageCode.INVALID_SIGNATURE -> InvalidMessageType.Signature
                else -> InvalidMessageType.Unrecognizable
            }
        }
    )
}