package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.messages.management.CallEndedMessage
import javax.inject.Inject

internal class CreateCallEndedMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageRequest) =
        with(request) {
            CallEndedMessage(
                msgId = message.msgId,
                time = message.timestamp,
                isMine = isMine,
                userHandle = message.userHandle,
                termCode = message.termCode,
                duration = message.duration.toLong(),
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                shouldShowDate = shouldShowDate,
            )
        }
}