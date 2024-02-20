package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.management.RetentionTimeUpdatedMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import javax.inject.Inject


internal class CreateRetentionTimeUpdatedMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override suspend fun invoke(request: CreateTypedMessageInfo) = with(request) {
        RetentionTimeUpdatedMessage(
            chatId = chatId,
            msgId = messageId,
            time = timestamp,
            isMine = isMine,
            userHandle = userHandle,
            retentionTime = retentionTime,
            shouldShowAvatar = shouldShowAvatar,
            shouldShowTime = shouldShowTime,
            reactions = reactions,
        )
    }
}