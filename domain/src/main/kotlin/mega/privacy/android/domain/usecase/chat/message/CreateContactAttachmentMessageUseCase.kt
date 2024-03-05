package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetUserUseCase
import javax.inject.Inject

internal class CreateContactAttachmentMessageUseCase @Inject constructor(
    private val getUserUseCase: GetUserUseCase,
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
) : CreateTypedMessageUseCase {

    override suspend fun invoke(request: CreateTypedMessageInfo) = with(request) {
        val contactHandle = userHandles.firstOrNull() ?: -1
        val isContact = getUserUseCase(UserId(contactHandle))
            ?.let { it.visibility == UserVisibility.Visible } ?: false
        ContactAttachmentMessage(
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
            contactEmail = userEmails.firstOrNull().orEmpty(),
            contactUserName = userNames.firstOrNull().orEmpty(),
            contactHandle = contactHandle,
            isMe = contactHandle != -1L && getMyUserHandleUseCase() == contactHandle,
            isContact = isContact,
        )
    }
}