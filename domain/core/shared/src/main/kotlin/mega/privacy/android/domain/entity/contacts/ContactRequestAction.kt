package mega.privacy.android.domain.entity.contacts

/**
 * Contact request action
 */
enum class ContactRequestAction {
    /**
     * Received Request: Accept
     */
    Accept,

    /**
     * Received Request: Deny
     */
    Deny,

    /**
     * Received Request: Ignore
     */
    Ignore,

    /**
     * Sent Request: Add
     */
    Add,

    /**
     * Sent Request: Delete
     */
    Delete,

    /**
     * Sent Request: Remind
     */
    Remind,
}
