package mega.privacy.android.app.presentation.meeting.chat.model.messages

/**
 * Result of inviting multiple users as contacts.
 */
sealed class InviteMultipleUsersAsContactResult {

    /**
     * Some users have already been requested and some have been sent successfully.
     *
     * @property alreadyRequested
     * @property sent
     */
    data class SomeAlreadyRequestedSomeSent(val alreadyRequested: Int, val sent: Int) :
        InviteMultipleUsersAsContactResult()

    /**
     * All users have already been requested.
     *
     * @property sent
     */
    data class AllSent(val sent: Int) : InviteMultipleUsersAsContactResult()

    /**
     * Some users have not been requested due to errors and some have been sent successfully.
     *
     * @property notSent
     * @property sent
     */
    data class SomeFailedSomeSent(val notSent: Int, val sent: Int) :
        InviteMultipleUsersAsContactResult()
}