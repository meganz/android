package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.messages.management.ChatLinkCreatedMessage
import javax.inject.Inject

internal class CreateChatLinkCreatedMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageRequest) =
        with(request) {
            ChatLinkCreatedMessage(
                msgId = message.msgId,
                time = message.timestamp,
                isMine = isMine,
                userHandle = message.userHandle,
                shouldShowAvatar = shouldShowAvatar,
                shouldShowTime = shouldShowTime,
                shouldShowDate = shouldShowDate,
            )
        }
}