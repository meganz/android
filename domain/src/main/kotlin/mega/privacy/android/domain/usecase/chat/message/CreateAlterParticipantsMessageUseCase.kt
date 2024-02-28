package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.management.AlterParticipantsMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import javax.inject.Inject

internal class CreateAlterParticipantsMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override suspend fun invoke(request: CreateTypedMessageInfo) = with(request) {
        AlterParticipantsMessage(
            chatId = chatId,
            msgId = messageId,
            time = timestamp,
            isDeletable = isDeletable,
            isEditable = isEditable,
            isMine = isMine,
            userHandle = userHandle,
            privilege = privilege,
            handleOfAction = handleOfAction,
            shouldShowAvatar = shouldShowAvatar,
            reactions = reactions,
            status = status,
            content = content,
        )
    }
}
