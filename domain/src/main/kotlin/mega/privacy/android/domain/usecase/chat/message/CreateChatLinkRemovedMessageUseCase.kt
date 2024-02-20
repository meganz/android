package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.management.ChatLinkRemovedMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import javax.inject.Inject

internal class CreateChatLinkRemovedMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override suspend fun invoke(request: CreateTypedMessageInfo) = with(request) {
        ChatLinkRemovedMessage(
            chatId = chatId,
            msgId = messageId,
            time = timestamp,
            isMine = isMine,
            userHandle = userHandle,
            shouldShowAvatar = shouldShowAvatar,
            shouldShowTime = shouldShowTime,
            reactions = reactions,
        )
    }
}