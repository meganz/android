package mega.privacy.android.domain.entity.contacts

/**
 * Enum class defining the possible request when invite a contact.
 */
enum class InviteContactRequest {

    /**
     * Invitation sent.
     */
    Sent,

    /**
     * Invitation resent.
     */
    Resent,

    /**
     * Invitation deleted.
     */
    Deleted,

    /**
     * Invitation already sent.
     */
    AlreadySent,

    /**
     * Already contact.
     */
    AlreadyContact,

    /**
     * Invalid email.
     */
    InvalidEmail,

    /**
     * Invalid status.
     */
    InvalidStatus
}