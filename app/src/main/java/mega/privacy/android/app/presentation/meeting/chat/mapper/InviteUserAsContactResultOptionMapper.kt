package mega.privacy.android.app.presentation.meeting.chat.mapper

import mega.privacy.android.app.presentation.meeting.chat.model.InviteUserAsContactResultOption
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import javax.inject.Inject

/**
 * Mapper for [InviteContactRequest] to [InviteUserAsContactResultOption]
 */
class InviteUserAsContactResultOptionMapper @Inject constructor() {
    /**
     *
     *
     * @param inviteContactRequest
     * @param email
     * @return [InviteUserAsContactResultOption]
     */
    operator fun invoke(
        inviteContactRequest: InviteContactRequest,
        email: String,
    ): InviteUserAsContactResultOption {
        return when (inviteContactRequest) {
            InviteContactRequest.Sent -> InviteUserAsContactResultOption.ContactInviteSent
            InviteContactRequest.AlreadySent, InviteContactRequest.AlreadyContact -> InviteUserAsContactResultOption
                .ContactAlreadyInvitedError(email = email)

            InviteContactRequest.InvalidEmail -> InviteUserAsContactResultOption.OwnEmailAsContactError
            else -> InviteUserAsContactResultOption.GeneralError
        }
    }
}