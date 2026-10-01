package mega.privacy.android.domain.entity.contacts

/**
 * Contact request status
 */
enum class ContactRequestStatus {
    /**
     * Unresolved
     */
    Unresolved,

    /**
     * Accepted
     */
    Accepted,

    /**
     * Denied
     */
    Denied,

    /**
     * Ignored
     */
    Ignored,

    /**
     * Deleted
     */
    Deleted,

    /**
     * Reminded
     */
    Reminded,
}
