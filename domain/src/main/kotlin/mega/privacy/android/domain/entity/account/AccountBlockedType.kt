package mega.privacy.android.domain.entity.account

/**
 * Enum class defining the types of blocked accounts.
 */
enum class AccountBlockedType {

    /**
     * Not blocked account.
     */
    NOT_BLOCKED,

    /**
     * Suspension only for multiple copyright violations.
     */
    TOS_COPYRIGHT,

    /**
     * Suspension message for any type of suspension, but copyright suspension.
     */
    TOS_NON_COPYRIGHT,

    /**
     * Subuser of the business account has been disabled.
     */
    SUBUSER_DISABLED,

    /**
     * Subuser of business account has been removed.
     */
    SUBUSER_REMOVED,

    /**
     * The account is temporary blocked and needs to be verified by an SMS code.
     */
    VERIFICATION_SMS,

    /**
     * The account is temporary blocked and needs to be verified by email (Weak Account Protection).
     */
    VERIFICATION_EMAIL,
}