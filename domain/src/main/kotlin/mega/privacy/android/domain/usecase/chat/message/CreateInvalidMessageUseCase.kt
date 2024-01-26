package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.ChatMessageCode
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.messages.invalid.FormatInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.InvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.SignatureInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.UnrecognizableInvalidMessage
import javax.inject.Inject

/**
 * Create invalid message use case.
 */
class CreateInvalidMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageRequest) =
        with(request) {
            val constructor: (
                Long,
                Long,
                Boolean,
                Long,
                Boolean,
                Boolean,
                Boolean,
            ) -> InvalidMessage = when {
                message.type == ChatMessageType.INVALID -> {
                    ::UnrecognizableInvalidMessage
                }

                message.code == ChatMessageCode.INVALID_FORMAT -> {
                    ::FormatInvalidMessage
                }

                message.code == ChatMessageCode.INVALID_SIGNATURE -> {
                    ::SignatureInvalidMessage
                }

                else -> {
                    ::UnrecognizableInvalidMessage
                }
            }

            constructor(
                message.msgId,
                message.timestamp,
                isMine,
                message.userHandle,
                shouldShowAvatar,
                shouldShowTime,
                shouldShowDate,
            )
        }


}