package mega.privacy.android.domain.entity.contacts

/**
 * An enum class represents the validation status of an inputted email that needs to be invited.
 */
enum class EmailInvitationsInputValidity {
    /**
     * The email is valid.
     */
    Valid,

    /**
     * The inputted email is the current logged in user's email.
     */
    MyOwnEmail,

    /**
     * The inputted email is already saved as a contact.
     */
    AlreadyInContacts,

    /**
     * The inputted email has been invited and is in a pending state.
     */
    Pending
}
