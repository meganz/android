package mega.privacy.android.app.presentation.meeting.chat.model

/**
 * All the possible results of inviting a user as a contact.
 *
 */
sealed interface InviteUserAsContactResultOption {

    /**
     * Request to invite a contact has been sent successfully
     */
    data object ContactInviteSent : InviteUserAsContactResultOption

    /**
     * There is already a pending invite request for this contact
     *
     * @property email Contact email
     */
    data class ContactAlreadyInvitedError(val email: String) : InviteUserAsContactResultOption

    /**
     * Current user wants to invite himself as a contact
     */
    data object OwnEmailAsContactError : InviteUserAsContactResultOption

    /**
     * General error
     */
    data object GeneralError : InviteUserAsContactResultOption
}
