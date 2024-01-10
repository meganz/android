package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.messages.management.CallStartedMessage
import javax.inject.Inject

internal class CreateCallStartedMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageRequest) =
        with(request) {
            CallStartedMessage(
                msgId = message.msgId,
                time = message.timestamp,
                isMine = isMine,
                userHandle = message.userHandle
            )
        }
}