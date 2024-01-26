package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.messages.management.PermissionChangeMessage
import javax.inject.Inject


internal class CreatePermissionChangeMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageRequest) = with(request) {
        PermissionChangeMessage(
            msgId = message.msgId,
            time = message.timestamp,
            isMine = isMine,
            userHandle = message.userHandle,
            privilege = message.privilege,
            handleOfAction = message.handleOfAction,
            shouldShowAvatar = shouldShowAvatar,
            shouldShowTime = shouldShowTime,
            shouldShowDate = shouldShowDate,
        )
    }
}