package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import javax.inject.Inject

internal class CreateContactAttachmentMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageInfo) = with(request) {
        ContactAttachmentMessage(
            msgId = msgId,
            time = timestamp,
            isMine = isMine,
            userHandle = userHandle,
            contactEmail = userEmails.firstOrNull().orEmpty(),
            contactUserName = userNames.firstOrNull().orEmpty(),
            contactHandle = userHandles.firstOrNull() ?: -1,
            shouldShowAvatar = shouldShowAvatar,
            shouldShowTime = shouldShowTime,
            shouldShowDate = shouldShowDate,
        )
    }
}