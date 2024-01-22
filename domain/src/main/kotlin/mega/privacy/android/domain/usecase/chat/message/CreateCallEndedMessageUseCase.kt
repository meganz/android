package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.management.CallEndedMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import javax.inject.Inject

internal class CreateCallEndedMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageInfo) =
        with(request) {
            CallEndedMessage(
                msgId = msgId,
                time = timestamp,
                isMine = isMine,
                userHandle = userHandle,
                termCode = termCode,
                duration = duration.toLong(),
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                shouldShowDate = shouldShowDate,
            )
        }
}