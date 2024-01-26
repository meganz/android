package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.messages.management.TitleChangeMessage
import javax.inject.Inject


internal class CreateTitleChangeMessageUseCase @Inject constructor() : CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageRequest) = with(request) {
        TitleChangeMessage(
            msgId = message.msgId,
            time = message.timestamp,
            isMine = isMine,
            userHandle = message.userHandle,
            content = message.content.orEmpty(),
            shouldShowAvatar = shouldShowAvatar,
            shouldShowTime = shouldShowTime,
            shouldShowDate = shouldShowDate,
        )
    }
}