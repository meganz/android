package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageInfo
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetUserUseCase
import javax.inject.Inject

internal class CreateContactAttachmentMessageUseCase @Inject constructor(
    private val getUserUseCase: GetUserUseCase,
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase,
) : CreateTypedMessageUseCase {

    override suspend fun invoke(request: CreateTypedMessageInfo) = with(request) {
        val contactHandle = userHandles.firstOrNull() ?: -1
        val user = getUserUseCase(UserId(contactHandle))
        val isContact = user?.let { it.visibility == UserVisibility.Visible } ?: false
        val isVerified = isContact && areCredentialsVerifiedUseCase(user?.email.orEmpty())
        ContactAttachmentMessage(
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
            contactEmail = userEmails.firstOrNull().orEmpty(),
            contactUserName = userNames.firstOrNull().orEmpty(),
            contactHandle = contactHandle,
            isMe = contactHandle != -1L && getMyUserHandleUseCase() == contactHandle,
            isContact = isContact,
            rowId = rowId,
            isVerified = isVerified
        )
    }
}