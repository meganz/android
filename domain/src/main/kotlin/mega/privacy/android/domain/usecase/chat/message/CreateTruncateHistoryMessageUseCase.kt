package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.management.TruncateHistoryMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import javax.inject.Inject

internal class CreateTruncateHistoryMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override suspend fun invoke(request: CreateTypedMessageInfo) = with(request) {
        TruncateHistoryMessage(
            chatId = chatId,
            msgId = messageId,
            time = timestamp,
            isDeletable = isDeletable,
            isEditable = isEditable,
            isMine = isMine,
            userHandle = userHandle,
            shouldShowAvatar = shouldShowAvatar,
            reactions = reactions,
            status = status,
            content = content,
        )
    }
}