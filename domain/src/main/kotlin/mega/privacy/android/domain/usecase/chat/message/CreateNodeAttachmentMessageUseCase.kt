package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import javax.inject.Inject


internal class CreateNodeAttachmentMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageInfo) = with(request) {
        NodeAttachmentMessage(
            msgId = msgId,
            time = timestamp,
            isMine = isMine,
            userHandle = userHandle,
            shouldShowAvatar = shouldShowAvatar,
            shouldShowTime = shouldShowTime,
            shouldShowDate = shouldShowDate,
        )
    }
}