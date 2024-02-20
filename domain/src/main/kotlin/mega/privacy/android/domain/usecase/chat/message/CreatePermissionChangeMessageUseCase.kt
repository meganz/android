package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.management.PermissionChangeMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import javax.inject.Inject


internal class CreatePermissionChangeMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override suspend fun invoke(request: CreateTypedMessageInfo) = with(request) {
        PermissionChangeMessage(
            chatId = chatId,
            msgId = messageId,
            time = timestamp,
            isMine = isMine,
            userHandle = userHandle,
            privilege = privilege,
            handleOfAction = handleOfAction,
            shouldShowAvatar = shouldShowAvatar,
            shouldShowTime = shouldShowTime,
            reactions = reactions,
        )
    }
}