package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageCode
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.messages.invalid.FormatInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.SignatureInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.UnrecognizableInvalidMessage
import javax.inject.Inject

/**
 * Create invalid message use case.
 */
class CreateInvalidMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(message: ChatMessage, isMine: Boolean) =
        when {
            message.type == ChatMessageType.INVALID -> {
                unrecognizableInvalidMessage(message, isMine)
            }

            message.code == ChatMessageCode.INVALID_FORMAT -> {
                formatInvalidMessage(message, isMine)
            }

            message.code == ChatMessageCode.INVALID_SIGNATURE -> {
                signatureInvalidMessage(message, isMine)
            }

            else -> {
                unrecognizableInvalidMessage(message, isMine)
            }
        }

    private fun unrecognizableInvalidMessage(
        message: ChatMessage,
        isMine: Boolean,
    ) = UnrecognizableInvalidMessage(
        msgId = message.msgId,
        time = message.timestamp,
        isMine = isMine,
        userHandle = message.userHandle,
    )

    private fun formatInvalidMessage(
        message: ChatMessage,
        isMine: Boolean,
    ) = FormatInvalidMessage(
        msgId = message.msgId,
        time = message.timestamp,
        isMine = isMine,
        userHandle = message.userHandle,
    )

    private fun signatureInvalidMessage(
        message: ChatMessage,
        isMine: Boolean,
    ) = SignatureInvalidMessage(
        msgId = message.msgId,
        time = message.timestamp,
        isMine = isMine,
        userHandle = message.userHandle,
    )

}