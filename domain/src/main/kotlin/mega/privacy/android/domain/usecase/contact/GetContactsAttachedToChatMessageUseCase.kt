package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.AttachedContact
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Get the contacts attached to a chat message.
 *
 * Replaces the legacy [nz.mega.sdk.MegaChatMessage] iteration that
 * ContactAttachmentActivity performed against the raw SDK, resolving each
 * attached user's contact visibility and verification state.
 */
class GetContactsAttachedToChatMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val getUserUseCase: GetUserUseCase,
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase,
) {

    /**
     * Invoke.
     *
     * @param chatId Chat id containing the message.
     * @param messageId Id of the message holding the contact attachments.
     * @return The attached contacts, or an empty list when the message cannot be found.
     */
    suspend operator fun invoke(chatId: Long, messageId: Long): List<AttachedContact> {
        val message = chatRepository.getMessage(chatId, messageId) ?: return emptyList()
        val myHandle = getMyUserHandleUseCase()
        return (0 until message.usersCount.toInt()).map { index ->
            val handle = message.userHandles.getOrNull(index) ?: -1L
            val email = message.userEmails.getOrNull(index).orEmpty()
            val name = message.userNames.getOrNull(index).orEmpty()
            val user = getUserUseCase(UserId(handle))
            val isContact = user?.visibility == UserVisibility.Visible
            val isVerified = isContact && areCredentialsVerifiedUseCase(email)
            AttachedContact(
                handle = handle,
                email = email,
                name = name,
                isMe = handle == myHandle,
                isContact = isContact,
                isVerified = isVerified,
            )
        }
    }
}
