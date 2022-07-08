package mega.privacy.android.domain.entity

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
