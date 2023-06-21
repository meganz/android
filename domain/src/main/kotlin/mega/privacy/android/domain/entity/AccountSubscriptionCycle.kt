package mega.privacy.android.domain.entity

/**
 * Enum class for renew cycle type (monthly/yearly) for account [AccountSubscriptionCycle]
 */
enum class AccountSubscriptionCycle {
    /**
     * MONTHLY
     */
    MONTHLY,

    /**
     * YEARLY
     */
    YEARLY,

    /**
     * UNKNOWN
     */
    UNKNOWN,
}