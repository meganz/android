package mega.privacy.android.domain.entity.user

/**
 * User visibility
 */
enum class UserVisibility {
    /**
     * Unknown visibility.
     */
    Unknown,

    /**
     * User has been a contact but it is not anymore.
     */
    Hidden,

    /**
     * User is a contact.
     */
    Visible,

    /**
     * User account does not exist anymore.
     */
    Inactive,

    /**
     * User blocked.
     */
    Blocked,
}