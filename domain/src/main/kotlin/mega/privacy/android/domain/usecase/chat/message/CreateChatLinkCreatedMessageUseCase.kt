package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.management.ChatLinkCreatedMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import javax.inject.Inject

internal class CreateChatLinkCreatedMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override suspend fun invoke(request: CreateTypedMessageInfo) =
        with(request) {
            ChatLinkCreatedMessage(
                chatId = chatId,
                msgId = messageId,
                time = timestamp,
                isDeletable = isDeletable,
                isEditable = isEditable,
                isMine = isMine,
                userHandle = userHandle,

                reactions = reactions,
                status = status,
                content = content,
            )
        }
}