package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.app.presentation.meeting.chat.mapper.InviteMultipleUsersAsContactResultMapper
import mega.privacy.android.app.presentation.meeting.chat.mapper.InviteUserAsContactResultMapper
import mega.privacy.android.app.presentation.meeting.chat.model.InviteUserAsContactResult
import mega.privacy.android.app.presentation.meeting.chat.model.messages.InviteMultipleUsersAsContactResult
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.usecase.contact.InviteContactWithHandleUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for contact attachment message.
 *
 * @property inviteContactWithHandleUseCase
 * @property inviteUserAsContactResultOptionMapper
 * @property inviteMultipleUsersAsContactResultMapper
 */
@HiltViewModel
class ContactAttachmentMessageViewModel @Inject constructor(
    private val inviteContactWithHandleUseCase: InviteContactWithHandleUseCase,
    private val inviteUserAsContactResultOptionMapper: InviteUserAsContactResultMapper,
    private val inviteMultipleUsersAsContactResultMapper: InviteMultipleUsersAsContactResultMapper,
) : ViewModel() {

    /**
     * Invites multiple contacts.
     *
     * @param messages
     * @return
     */
    suspend fun inviteMultipleContacts(messages: Set<ContactAttachmentMessage>)
            : InviteMultipleUsersAsContactResult = coroutineScope {
        messages.map { message ->
            async {
                inviteContactWithHandleUseCase(message.contactEmail, message.contactHandle, null)
            }
        }.awaitAll().let { inviteMultipleUsersAsContactResultMapper(it) }
    }


    /**
     * Invites a contact.
     *
     * @param message
     * @return
     */
    suspend fun inviteContact(
        message: ContactAttachmentMessage,
    ): InviteUserAsContactResult {
        runCatching {
            val email = message.contactEmail
            val result = inviteContactWithHandleUseCase(
                email = email,
                handle = message.contactHandle,
                message = null
            )
            return inviteUserAsContactResultOptionMapper(
                inviteContactRequest = result,
                email = message.contactEmail

            )
        }.onFailure {
            Timber.e(it)
        }
        return InviteUserAsContactResult.GeneralError
    }
}
