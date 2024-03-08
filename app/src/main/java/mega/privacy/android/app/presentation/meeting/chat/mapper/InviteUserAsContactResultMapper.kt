package mega.privacy.android.app.presentation.meeting.chat.mapper

import mega.privacy.android.app.presentation.meeting.chat.model.InviteUserAsContactResult
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import javax.inject.Inject

/**
 * Mapper for [InviteContactRequest] to [InviteUserAsContactResult]
 */
class InviteUserAsContactResultMapper @Inject constructor() {
    /**
     *
     *
     * @param inviteContactRequest
     * @param email
     * @return [InviteUserAsContactResult]
     */
    operator fun invoke(
        inviteContactRequest: InviteContactRequest,
        email: String,
    ): InviteUserAsContactResult {
        return when (inviteContactRequest) {
            InviteContactRequest.Sent -> InviteUserAsContactResult.ContactInviteSent
            InviteContactRequest.AlreadySent, InviteContactRequest.AlreadyContact -> InviteUserAsContactResult
                .ContactAlreadyInvitedError(email = email)

            InviteContactRequest.InvalidEmail -> InviteUserAsContactResult.OwnEmailAsContactError
            else -> InviteUserAsContactResult.GeneralError
        }
    }
}