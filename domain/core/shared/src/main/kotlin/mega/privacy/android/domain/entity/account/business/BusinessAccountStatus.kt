package mega.privacy.android.domain.entity.account.business

/**
 * Business account status
 */
enum class BusinessAccountStatus {
    /**
     * Expired
     */
    Expired,

    /**
     * Inactive
     */
    Inactive,

    /**
     * Active
     */
    Active,

    /**
     * Grace period
     */
    GracePeriod,
}
