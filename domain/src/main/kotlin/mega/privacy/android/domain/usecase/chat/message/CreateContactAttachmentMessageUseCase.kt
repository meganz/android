package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import javax.inject.Inject

internal class CreateContactAttachmentMessageUseCase @Inject constructor() :
    CreateTypedMessageUseCase {

    override fun invoke(request: CreateTypedMessageRequest) = with(request) {
        ContactAttachmentMessage(
            msgId = message.msgId,
            time = message.timestamp,
            isMine = isMine,
            userHandle = message.userHandle,
            contactEmail = message.userEmails.firstOrNull().orEmpty(),
            contactUserName = message.userNames.firstOrNull().orEmpty(),
            contactHandle = message.userHandles.firstOrNull() ?: -1,
            shouldShowAvatar = shouldShowAvatar,
            shouldShowTime = shouldShowTime,
            shouldShowDate = shouldShowDate,
        )
    }
}